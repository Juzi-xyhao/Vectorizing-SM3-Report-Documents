<a name="VO6kA"></a>
## SM3算法介绍
**SM3密码杂凑算法**是中国国家密码管理局2010年公布的中国商用密码杂凑算法标准。具体算法标准原始文本参见参考文献[1]。该算法于2012年发布为密码行业标准(GM/T 0004-2012)，2016年发布为国家密码杂凑算法标准(GB/T 32905-2016)。<br />SM3适用于商用密码应用中的数字签名和验证，是在[SHA-256]基础上改进实现的一种算法，其安全性和SHA-256相当。SM3和MD5的迭代过程类似，也采用Merkle-Damgard结构。消息分组长度为512位，摘要值长度为256位。摘要长度比MD5长一倍。安全性更高。<br />SM3加密算法的整体流程是：

1. 将需要加密的消息先填充，再转换为字节数组
2. 将输入的字节数组每512位一划分，作为一个块（16个int）。对块的处理需要先扩展再压缩
3. 扩展阶段需要生成68个消息字w[]，64个辅助消息字w`[]。消息字的前16位照搬块中的16个int，后52位每一位W[j]按照公式计算。辅助消息字W`[j]也按照公式计算。
4. 压缩阶段，有ABCDEFGH共8个int类型的寄存器，SS1，SS2,TT1.TT2共四个中间变量，每次迭代更新ABCDEFGH的值
5. 将最后一轮迭代保存的ABCDEFGH8个整数，转化为字节数组。每个整数可以转化为4个字节共32字节，也就是64个16进制数

整个算法最耗时的步骤就是扩展和压缩了。<br />扩展阶段的生成公式为：
```java
w[i] = p1(w[i - 16] ^ w[i - 9] ^ circularLeftShift(w[i - 3], 15))
        ^ circularLeftShift(w[i - 13], 7)
        ^ w[i - 6];

w`[i] = w[i] ^ w[i + 4];


private static int p1(int x) {
    return x ^ circularLeftShift(x, 15)
    ^ circularLeftShift(x, 23);
}

public static int circularLeftShift(int n, int bits) {
    return (n << bits) | (n >>> (32 - bits));
}
```
压缩阶段比较复杂
```java
private void compress() {
    int a = v[0];
    int b = v[1];
    int c = v[2];
    int d = v[3];
    int e = v[4];
    int f = v[5];
    int g = v[6];
    int h = v[7];

    for (int i = 0; i < 16; i++) {
        int a12 = circularLeftShift(a, 12);
        int ss1 = circularLeftShift(a12 + e + T[i], 7);
        int ss2 = ss1 ^ a12;

        int tt1 = ff0(a, b, c) + d + ss2 + (w[i] ^ w[i + 4]); // w`[i] = w[i] ^ w[i + 4]
        int tt2 = gg0(e, f, g) + h + ss1 + w[i];

        d = c;
        c = circularLeftShift(b, 9);
        b = a;
        a = tt1;
        h = g;
        g = circularLeftShift(f, 19);
        f = e;
        e = p0(tt2);
    }

    for (int i = 16; i < 64; i++) {
        int a12 = circularLeftShift(a, 12);
        int ss1 = circularLeftShift(a12 + e + T[i], 7);
        int ss2 = ss1 ^ a12;

        int tt1 = ff1(a, b, c) + d + ss2 + (w[i] ^ w[i + 4]); // w`[i] = w[i] ^ w[i + 4]
        int tt2 = gg1(e, f, g) + h + ss1 + w[i];

        d = c;
        c = circularLeftShift(b, 9);
        b = a;
        a = tt1;
        h = g;
        g = circularLeftShift(f, 19);
        f = e;
        e = p0(tt2);
    }

    v[0] ^= a;
    v[1] ^= b;
    v[2] ^= c;
    v[3] ^= d;
    v[4] ^= e;
    v[5] ^= f;
    v[6] ^= g;
    v[7] ^= h;
}
```
最后一轮迭代中，v数组中的元素就是压缩结果。其中八个int共256位。全部转化为字节以16进制表示即为SM3算法的加密结果。

<a name="CR9X2"></a>
## Vector API的优化思路
[Vector API介绍、简单使用](https://www.yuque.com/u41117719/xd1qgc/nobqkb0q817ciide)
<a name="ybUcf"></a>
### 扩展阶段优化
```java
w[i] = p1(w[i - 16] ^ w[i - 9] ^ circularLeftShift(w[i - 3], 15))
        ^ circularLeftShift(w[i - 13], 7)
        ^ w[i - 6];

w`[i] = w[i] ^ w[i + 4];
```
Species的长度为64字节的情况下，一个int类型的向量包含16个int。也就是SPECIES.length() = 16<br />
如果将w[i]到w[i + SPECIES.length()]作为一个向量去运算的话，那么w[i - 16],w[i - 3]<br />等w数组中的元素也要向后取SPECIES.length()个元素作为向量<br />
但这样会有问题，w[i]到w[i + SPECIES.length()]这个向量是作为运算结果的，是未知量，未初始化。<br />![](/assets/img_4.png)<br />
如上图所示：<br />运算数中的向量w[i - 7]只有前7个元素是已知的，w[i - 3]只有前三个元素是已知的。后面的元素还是未知量<br />
这相当于拿着未知量去计算未知量，结果显然错误<br />因此只能缩短向量的长度，但是这样一来性能反而还不如不用向量化。<br />
所以Vector API在扩展这一步中，对w[i]数组的扩展，并没有起到优化的作用<br />
但对于w`[i]数组而言，整个w数组都是已知的，构造一个w[i]到w[i + 16]的向量，一个w[i + 4]到w[i + 20]的向量。

这两个向量异或的结果一一对应了w`[i]到w`[i + 16]。这是一个优化的思路。

<a name="nCBQx"></a>
### 压缩阶段优化
压缩阶段主要是v数组中的8个元素互相异或交换。扩展阶段中还能在一个数组中分出多个向量模拟多向量运算。但在这一步中向量化没有操作空间。

<a name="NzE32"></a>
## 总结
![image.png](/assets/img_5.png)<br />这是按照上文中的优化思路重构的实现代码与原实现代码的比较。重构之后反而时间消耗大大增加。<br />
性能测试应该是要用JMH测试的，但是由于Vector API特殊性，至今我也没能让代码在idea中跑起来，命令行中启动又会报各种各样的解决不了的错误。导致JMH测试的代码已经写好却跑不了。<br />
但鉴于在命令行对两种方法的直接测试的时间消耗结果差异巨大（差距达到四百多倍），JMH测试的结果也八九不离十了。<br />
经过这一番对Vector API的了解与应用之后，我认为**Vector API不适合单数组的自我运算**。自然也就优化不了SM3算法了。
