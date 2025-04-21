
## 背景
发布到中央仓库时需要javadoc注释，所以写了这个maven插件用于在构建阶段自动生成注释

## 特性
- 仅支持java17以上版本
- 自动填充类注释
- 自动填充javadoc注释


## 使用
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>0.5.0</version>
            <extensions>true</extensions>
            <configuration>
                <publishingServerId>xxx</publishingServerId>
                <checksums>required</checksums>
                <deploymentName>xxx</deploymentName>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
            <version>3.2.1</version>
            <executions>
                <execution>
                    <id>attach-sources</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>jar-no-fork</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>io.github.liyao52033</groupId>
            <artifactId>autofill-javadoc-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>autofill</goal>
                    </goals>
                    <phase>generate-sources</phase> <!-- 确保在javadoc之前运行 -->
                </execution>
            </executions>
            <configuration>
                <sourceDir>src/main/java</sourceDir> <!-- 指定源代码目录 -->
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>3.5.0</version>
            <configuration>
                <encoding>UTF-8</encoding>
                <charset>UTF-8</charset>
                <docencoding>UTF-8</docencoding>
            </configuration>
            <executions>
                <execution>
                    <id>attach-javadocs</id>
                    <goals>
                        <goal>jar</goal>
                    </goals>
                    <phase>verify</phase> <!-- Javadoc 生成在 verify 阶段运行 -->
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <version>3.2.7</version>
            <executions>
                <execution>
                    <id>sign-artifacts</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>sign</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <gpgArguments>
                    <arg>--pinentry-mode</arg>
                    <arg>loopback</arg>
                </gpgArguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```