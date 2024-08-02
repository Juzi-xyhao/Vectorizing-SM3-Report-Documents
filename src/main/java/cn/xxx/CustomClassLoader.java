package cn.xxx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomClassLoader extends ClassLoader {

    private final Path classDir;

    // 构造函数，接受类文件所在的目录路径
    public CustomClassLoader(String classDir) {
        this.classDir = Paths.get(classDir);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // 将类名转换为文件路径
            String classFileName = name.replace('.', '/') + ".class";
            Path classFilePath = classDir.resolve(classFileName);

            // 读取类的字节码
            byte[] classBytes = Files.readAllBytes(classFilePath);

            // 定义并返回类
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("无法加载类 " + name, e);
        }
    }
}

