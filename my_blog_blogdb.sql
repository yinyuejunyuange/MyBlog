-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: my_blog_blogdb
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `blog`
--

DROP TABLE IF EXISTS `blog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title` varchar(64) NOT NULL COMMENT '文章标题',
  `user_id` bigint NOT NULL COMMENT '作者id',
  `context` text NOT NULL COMMENT '内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '文章状态 1：保存中 2：发布 3：审核中 4：禁止查看',
  `is_delete` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `introduce` varchar(256) DEFAULT NULL COMMENT '文章简介',
  `kudos` bigint DEFAULT '0' COMMENT '博客点在数',
  `star` bigint DEFAULT '0' COMMENT '博客收藏数',
  `watch` bigint DEFAULT '0' COMMENT '博客阅读数',
  `comment_num` bigint DEFAULT '0' COMMENT '博客评论数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1897232938253422595 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blog`
--

LOCK TABLES `blog` WRITE;
/*!40000 ALTER TABLE `blog` DISABLE KEYS */;
INSERT INTO `blog` VALUES (1893564743697141761,'',1,'','2025-02-23 07:33:10','2025-02-23 07:33:10',1,0,'',0,0,3,0),(1893565229871501314,'',1,'','2025-02-23 07:35:06','2025-02-23 07:35:06',1,0,'',0,0,4,0),(1893566009848467457,'123',1,'<h2><strong>sadsadf</strong></h2><p>asdfsadfsdfsda</p>','2025-02-23 07:38:12','2025-02-23 07:38:12',1,0,'<h2><strong>sadsadf</strong></h2><p>asdfsadfsdfsda</p>',0,0,6,0),(1893581843467694082,'我的第一个博客',1,'<p>我的第一个博客</p>','2025-02-23 08:41:07','2025-02-23 08:41:07',1,0,'我的第一个博客',2,2,40,11),(1893845854478217218,'abc',1,'<p><img src=\"http://localhost:8082/blog/file/download/eea064ec-8image.png\" alt=\"\" data-href=\"\" style=\"\"/></p>','2025-02-24 02:10:12','2025-02-24 02:10:12',1,0,'abc',0,1,4,0),(1893848556310441986,'abc',1,'<p><img src=\"http://localhost:8082/blog/file/download/a6f5df51-3qq3.jpg\" alt=\"\" data-href=\"\" style=\"\"/>hello</p>','2025-02-24 02:20:56','2025-02-24 02:20:56',1,0,'abc\n',0,1,3,0),(1893899817365835778,'test01',12,'test01','2025-02-24 05:44:38','2025-02-24 05:44:38',1,0,NULL,0,0,0,0),(1893903589265674241,'test02',1,'test02','2025-02-24 05:59:37','2025-02-24 05:59:37',1,0,NULL,0,0,0,0),(1893977720774459394,'test02',1,'test02','2025-02-24 10:54:12','2025-02-24 10:54:12',1,0,NULL,0,0,2,0),(1894011652698021889,'test04',1,'test04','2025-02-24 13:09:02','2025-02-24 13:09:02',1,0,NULL,0,1,1,0),(1894939800474304514,'Github pull request详细教程（提交代码到他人仓库）',1,'<p>首先，我们需要明确当修改他人仓库代码时所需要的步骤：</p><p><br></p><p>先 fork 别人的仓库，相当于拷贝一份到自己的GitHub地址</p><p>将仓库克隆到本地，创建一个仓库分支，将原代码基础上修改后的代码全部放到仓库分支中。</p><p>发起 pull request 到原仓库，让原作者本人看到你的修改。</p><p>原代码作者看到你的request后，会检查你的代码，如果他觉得正确，会review到自己的项目中。</p><p>这就是整个pull request的全过程。</p><p><br></p><p>下面来看具体步骤：</p><p><br></p><p>1.fork他人仓库到自己账号</p><p><br></p><p><br></p><p>2.将仓库clone到本地</p><p><br></p><p><br></p><p>3.打开Git Bash，进入本地仓库地址</p><p>~ &nbsp;cd +本地地址</p><p>1</p><p><br></p><p><br></p><p>4.创建新分支，将修改后的本地仓库搬运到分支中</p><p>~ &nbsp;git checkout -b 分支名</p><p>~ &nbsp;git add .</p><p>~ &nbsp;git commit -m \"xxxxx\"</p><p>~ &nbsp;git push origin 分支名</p><p>1</p><p>2</p><p>3</p><p>4</p><p>5.完成后，进入你fork的仓库，找到该分支</p><p><img src=\"http://localhost:8080/myBlog/user/blog/file/download/4a86f2f0-aimage.png\" alt=\"\" data-href=\"\" style=\"\"/></p><p><br></p><p>至此，pull request就完成了，我们只需要等待原仓库主人进行审核。</p><p><br></p><p>附（pull request 过程中可能出现的其他问题）：</p><p>github显示There isn’t anything to compare</p><p>pull request过程中发现创建的分支传错仓库</p><p>————————————————</p><p><br></p><p> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;版权声明：本文为博主原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接和本声明。</p><p> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</p><p>原文链接：https://blog.csdn.net/CY2333333/article/details/113731490</p>','2025-02-27 02:37:09','2025-02-27 02:37:09',1,0,'Github pull request详细教程（提交代码到他人仓库）',0,1,35,2),(1896899273258549249,'全民AI时代：手把手教你用Ollama & AnythingLLM搭建AI知识库，无需编程，跟着做就行！',1,'<p>第一步：安装ollam</p><p>ollam，这玩意儿现在可以说是跑大模型的神器。不管你用的是PC、Mac还是树莓派，只要内存够，跑起来都不是问题。记得，7B模型至少要8G内存，13B的要16G，想玩70B的大家伙，那得有64G。首先，去ollama.com下载适合你操作系统的版本，我用的是Windows，下载安装完之后，在命令行敲一下ollama -V，能看到版本号就说明安装好了。</p><p>下载适合本地电脑的大模型，ollama网站上有个“Models”链接，点进去找到“gemma”模型。在“Tags”里面找到你想运行的模型版本，比如“7b”，旁边有个复制按钮，点击后把命令复制下来。然后就是在命令行里跑这个命令，第一次运行会自动下载模型。下载完毕，就能开始和它聊天或者让它帮你写代码了。</p><p><br></p><p><br></p><p>终端输入：</p><p><br></p><p>ollama run llama2:7b</p><p><br></p><p>1</p><p>2</p><p><br></p><p><br></p><p>下载完成后，就进入交互模式，这就可以开始聊天了。</p><p><br></p><p><br></p><p><br></p><p>至此，Gemma 模型的_本地部署运行已经完成了_，非常简单。</p><p><br></p><p>使用像 ChatGPT 那样的现代的聊天窗口</p><p>虽然部署完成了，但是上面那种_古老的终端_可能影响我们的交互体验，下面提供两个现代聊天窗口工具（如果你不想使用这些GUI，这一步可以跳过，不影响本地知识库搭建，**直接去看下面的第二步：**AnythingLLM安装）：chatbox 和 openai web ui。</p><p><br></p><p>第一个：chatbox</p><p><br></p><p>打开设置，模型提供方选 ollama，API 地址是本地的 11434 端口，模型选 gemma:7b。</p><p><br></p><p>地址：https://chatboxai.app/zh</p><p><br></p><p>第二个：openai web ui</p><p><br></p><p>如何安装🚀</p><p>请注意，对于某些 Docker 环境，可能需要额外的配置。如果您遇到任何连接问题，我们有关Open WebUI 文档的详细指南随时可以为您提供帮助。</p><p><br></p><p><br></p><p><br></p><p>快速开始使用 Docker 🐳</p><p>使用 Docker 安装 Open WebUI 时，请确保 -v open-webui:/app/backend/data 在 Docker 命令中包含 。此步骤至关重要，因为它确保您的数据库正确安装并防止任何数据丢失。</p><p><br></p><p>• 如果 Ollama 在您的计算机上，请使用以下命令：</p><p>docker run -d -p 3000:8080 --add-host=host.docker.internal:host-gateway -v open-webui:/app/backend/data --name open-webui --restart always ghcr.io/open-webui/open-webui:main</p><p><br></p><p>1</p><p>2</p><p>• 如果 Ollama 位于不同的服务器上，请使用以下命令：</p><p># 要连接到另一台服务器上的 Ollama，请将 更改 OLLAMA_BASE_URL 为服务器的 URL： &nbsp; docker run -d -p 3000:8080 -e OLLAMA_BASE_URL=https://example.com -v open-webui:/app/backend/data --name open-webui --restart always ghcr.io/open-webui/open-webui:main</p><p><br></p><p>1</p><p>2</p><p>安装完成后，您可以通过 http://localhost:3000 访问 Open WebUI。享受！😄</p><p><br></p><p>打开 WebUI：服务器连接错误</p><p>如果您遇到连接问题，通常是由于 WebUI docker 容器无法访问容器内位于 127.0.0.1:11434 (host.docker.internal:11434) 的 Ollama 服务器。使用 --network=hostdocker 命令中的标志来解决此问题。请注意，端口从 3000 更改为 8080，导致链接：http://localhost:8080。</p><p><br></p><p>Docker 命令示例：</p><p><br></p><p>docker run -d --network=host -v open-webui:/app/backend/data -e OLLAMA_BASE_URL=http://127.0.0.1:11434 --name open-webui --restart always ghcr.io/open-webui/open-webui:main</p><p><img src=\"http://localhost:8080/myBlog/user/blog/file/download/43bf6b89-2image.png\" alt=\"\" data-href=\"\" style=\"\"/></p><p>1</p><p>2</p><p>其他安装方法</p><p>我们提供各种安装替代方案，<img src=\"http://localhost:8080/myBlog/user/blog/file/download/045c898f-a流萤.jpg\" alt=\"\" data-href=\"\" style=\"\"/>包括非 Docker 方法、Docker Compose、Kustomize 和 Helm。请访问我们的 Open WebUI 文档或加入我们的 Discord 社区以获得全面的指导。</p><p><br></p>','2025-03-04 12:23:24','2025-03-04 12:23:24',1,0,'在本地电脑上跑大语言模型（LLM），已经不是什么高科技操作了。随着技术的迭代，现在利用Ollam和AnythingLLM就可以轻松构建自己的本地知识库，人人皆可上手，有手就行。过往要达成这一目标，可是需要有编程经验的。',1,1,3,3),(1897126754930515970,'一文吃透 Java 类加载机制',1,'<h2 style=\"text-align: start;\">一、类加载的时机<br></h2><p>很多数人在问 “类什么时候加载” 和 “类什么时候初始化”，从语境上来说，都是在问同一个问题，就是这个.class 文件什么时候被读取到虚拟机的内存中，并且达到可用的状态。但严格意义上来说，加载和初始化，是类生命周期的两个阶段。<br></p><p>对于什么时候加载，java 虚拟机规范中并没有约束，各个虚拟机都可以按自身需要来自由实现。但绝大多数情况下，都遵循 “什么时候初始化” 来进行加载。<br></p><p>什么时候初始化？java 虚拟机规范有明确规定，当符合以下条件时（包括但不限于），虚拟机内存中没有找到对应类型信息，则必须对类进行 “初始化” 操作：<br></p><ol><li style=\"text-align: start;\">使用 new 实例化对象时、读取或者设置一个类的静态字段或方法时</li><li style=\"text-align: start;\">反射调用时，例如 class.forname (\"com.xxx.testdemo\")</li><li style=\"text-align: start;\">初始化一个类的子类，会首先初始化子类的父类</li><li style=\"text-align: start;\">java 虚拟机启动时标明的启动类</li><li style=\"text-align: start;\">jdk8 之后，接口中存在 default 方法，这个接口的实现类初始化时，接口会其之前进行初始化<br></li><li style=\"text-align: start;\">初始化阶段开始之前，自然还是要先经历加载、验证、准备、解析的。</li><li style=\"text-align: start;\"></li></ol><pre style=\"text-align: start; line-height: 1.5;\"><code>public static int value = 3</code></pre><p style=\"text-align: start;\"><br></p>','2025-03-05 03:27:20','2025-03-05 03:27:20',1,0,' Java 的世界里，每一个类或者接口，在经历编译器后，都会生成一个个.class 文件。类加载机制指的是将这些.class 文件中的二进制数据读入到内存中，并对数据进行校验，解析和初始化。最终，每一个类都会在方法区保存一份它的元数据，在堆中创建一个与之对应的 class 对象。\n类的生命周期，经历 7 个阶段，分别是加载、验证、准备、解析、初始化、使用、卸载。除了使用和卸载两个过程，前面的 5 个阶段加载、验证、准备、解析、初始化的执行过程，就是类的加载过程。',0,0,2,0),(1897213872725823490,'ai测试1',1,'<p>ai文档上传测试01</p><p><img src=\"http://localhost:8080/myBlog/user/blog/file/download/74df4337-6image.png\" alt=\"\" data-href=\"\" style=\"\"/></p><p>啊啊啊去去去</p>','2025-03-05 09:13:30','2025-03-05 09:13:30',1,0,'ai测试1',0,0,0,0),(1897215538200055810,'ai测试1',1,'<p>ai文档上传测试01</p><p><img src=\"http://localhost:8080/myBlog/user/blog/file/download/74df4337-6image.png\" alt=\"\" data-href=\"\" style=\"\"/></p><p>啊啊啊去去去</p>','2025-03-05 09:20:08','2025-03-05 09:20:08',1,0,'ai测试1',0,0,0,0),(1897215757268553729,'ai测试1',1,'<p>ai文档上传测试01</p><p><img src=\"http://localhost:8080/myBlog/user/blog/file/download/74df4337-6image.png\" alt=\"\" data-href=\"\" style=\"\"/></p><p>啊啊啊去去去</p>','2025-03-05 09:21:00','2025-03-05 09:21:00',1,0,'ai测试1',0,0,0,0),(1897217720592896001,'ai文档上传测试01',1,'<p>文档上传测试</p>','2025-03-05 09:28:48','2025-03-05 09:28:48',1,0,'文档上传测试',0,0,0,0),(1897221387832266754,'ai文档上传测试02',1,'<p>文档上传测试2</p>','2025-03-05 09:43:22','2025-03-05 09:43:22',1,0,'文档上传测试2',0,0,0,0),(1897224059738132481,'ai文档上传测试03',1,'<p>文档上传测试3<img src=\"http://localhost:8080/myBlog/user/blog/file/download/7d526d1e-eimage.png\" alt=\"\" data-href=\"\" style=\"\"/></p>','2025-03-05 09:53:59','2025-03-05 09:53:59',1,0,'文档上传测试2',0,0,0,0),(1897225375055089666,'ai文档上传测试04',1,'<p>文档上传测试4<img src=\"http://localhost:8080/myBlog/user/blog/file/download/7d526d1e-eimage.png\" alt=\"\" data-href=\"\" style=\"\"/></p>','2025-03-05 09:59:13','2025-03-05 09:59:13',1,0,'文档上传测试4',0,0,0,0),(1897226669299863553,'ai文档上传测试05',1,'<p>文档上传测试5<img src=\"http://localhost:8080/myBlog/user/blog/file/download/7d526d1e-eimage.png\" alt=\"\" data-href=\"\" style=\"\"/></p>','2025-03-05 10:04:21','2025-03-05 10:04:21',1,0,'文档上传测试5',0,0,0,0),(1897228415841603585,'ai文档上传测试06',1,'<p>文档上传测试6<img src=\"http://localhost:8080/myBlog/user/blog/file/download/7d526d1e-eimage.png\" alt=\"\" data-href=\"\" style=\"\"/></p>','2025-03-05 10:11:18','2025-03-05 10:11:18',1,0,'文档上传测试6',0,0,0,0),(1897230261939343361,'Java 反射基础原理剖析',1,'<p><br></p><p>在 Java 的世界里，反射是一项独特且强大的机制。它赋予了程序在运行时检查和操作自身结构的能力。简单来说，通过反射，我们可以在运行时获取类的信息、创建对象、调用方法以及访问字段。</p><p>每个 Java 类在被加载到 JVM 中时，都会对应一个Class对象。这个Class对象就像是类的 “信息仓库”，包含了类的名称、字段、方法、构造函数等所有信息。获取Class对象是使用反射的第一步，常见的方式有三种：</p><ol><li>通过对象的getClass方法：</li><li></li></ol><pre><code >MyClass myObj = new MyClass();Class&lt;?&gt; clazz = myObj.getClass();</code></pre><ol><li>通过类名.class方式：</li><li></li></ol><pre><code >Class&lt;?&gt; clazz = MyClass.class;</code></pre><ol><li>通过Class.forName方法，传入类的全限定名：</li><li></li></ol><pre><code >Class&lt;?&gt; clazz = Class.forName(\"com.example.MyClass\");\n有了Class对象后，我们可以进一步获取类的构造函数。例如，获取所有公有的构造函数：\n</code></pre><pre><code >Constructor&lt;?&gt;[] constructors = clazz.getConstructors();for (Constructor&lt;?&gt; constructor : constructors) {    System.out.println(constructor);}\n也可以获取特定参数类型的构造函数来创建对象：\n</code></pre><pre><code >Constructor&lt;?&gt; constructor = clazz.getConstructor(String.class, int.class);Object obj = constructor.newInstance(\"参数值\", 10);\n同样地，我们能获取类的方法并调用，以及访问类的字段：\n</code></pre><pre><code >// 获取方法Method method = clazz.getMethod(\"methodName\", parameterTypes);method.invoke(obj, args);// 获取字段Field field = clazz.getField(\"fieldName\");Object fieldValue = field.get(obj);\n反射的基础原理就在于通过Class对象，在运行时动态地探索和操作类的各个部分，为程序带来了极大的灵活性。\n</code></pre><h2>博客文章二：Java 反射的丰富应用场景</h2><p>Java 反射机制在众多实际场景中都发挥着关键作用，为开发者提供了强大的工具来构建更灵活、可扩展的应用。</p><p><br></p><h3>框架开发</h3><p>以 Spring 框架为例，其核心的依赖注入（DI）和面向切面编程（AOP）功能都离不开反射。在 DI 中，Spring 通过读取配置文件（如 XML 或注解），利用反射动态创建对象并注入依赖。例如，当配置了一个 Bean，Spring 会根据类名使用反射创建对象：</p><p><br></p><pre><code >// 假设配置的类名为com.example.MyServiceClass&lt;?&gt; clazz = Class.forName(\"com.example.MyService\");Object myService = clazz.newInstance();\n在 AOP 中，反射用于在运行时动态代理目标对象，织入切面逻辑，实现日志记录、事务管理等功能。\n</code></pre><h3>插件化架构</h3><p>许多大型应用希望具备插件化能力，允许用户在不修改核心代码的情况下添加新功能。通过反射，应用可以在运行时加载外部插件类。比如，一个绘图软件可能允许用户安装不同的绘图算法插件。应用读取插件配置文件，获取插件类名，然后使用反射加载并实例化插件类：</p><p><br></p><pre><code >String pluginClassName = readPluginConfig(\"plugin1\");Class&lt;?&gt; pluginClass = Class.forName(pluginClassName);Plugin plugin = (Plugin) pluginClass.newInstance();plugin.execute();\n</code></pre><h3>单元测试</h3><p>在单元测试中，有时需要测试私有方法或访问私有字段。反射可以突破访问限制，让测试代码能够调用私有方法、设置私有字段的值，从而全面测试类的功能。例如：</p><p><br></p><pre><code >// 测试私有方法Class&lt;?&gt; clazz = TargetClass.class;Method privateMethod = clazz.getDeclaredMethod(\"privateMethod\", parameterTypes);privateMethod.setAccessible(true);privateMethod.invoke(targetObject, args);// 测试私有字段Field privateField = clazz.getDeclaredField(\"privateField\");privateField.setAccessible(true);privateField.set(targetObject, value);\n此外，反射在注解处理、对象序列化与反序列化等场景中也有着广泛应用，它让 Java 程序在运行时的行为更加动态和可定制。\n</code></pre><h2>博客文章三：Java 反射性能优化策略</h2><p>虽然 Java 反射机制功能强大，但它也存在性能开销的问题。由于反射操作涉及运行时的类型检查、动态创建对象和方法调用，相比直接的代码调用，性能会有明显下降。不过，我们可以采取一些策略来优化反射性能。</p><p><br></p><h3>减少反射调用次数</h3><p>尽量避免在循环中频繁使用反射。例如，如果需要多次调用一个反射方法，考虑将反射获取的Method对象缓存起来，而不是每次都重新获取。</p><p><br></p><pre><code >// 错误示范：每次循环都获取Method对象for (int i = 0; i &lt; 1000; i++) {    Class&lt;?&gt; clazz = MyClass.class;    Method method = clazz.getMethod(\"methodName\");    method.invoke(myObject);}// 正确示范：缓存Method对象Class&lt;?&gt; clazz = MyClass.class;Method method = clazz.getMethod(\"methodName\");for (int i = 0; i &lt; 1000; i++) {    method.invoke(myObject);}\n</code></pre><h3>使用 JVM 反射优化机制</h3><p>JVM 自身对反射有一定的优化措施。当一个反射方法调用次数达到阈值（默认是 15 次）时，JVM 会自动对该调用进行优化，内部会转换为对某个类或实例方法的直接调用，从而提高效率。我们可以通过设置 JVM 属性来控制这一优化过程。例如，sun.reflect.noInflation属性可以控制是否直接生成动态类来优化反射调用，sun.reflect.inflationThreshold属性可以设置生成动态类的阈值。在 Idea 中，可以在运行配置的 VM options 中设置这些属性：</p><p><br></p><pre><code >-Dsun.reflect.noInflation=true-Dsun.reflect.inflationThreshold=10\n</code></pre><h3>使用反射工具类</h3><p>一些第三方反射工具类，如 Apache Commons BeanUtils，对反射操作进行了封装和优化。它们通常会缓存反射信息，减少重复的反射查找，从而提高性能。例如，使用 BeanUtils 获取对象属性值：</p><p><br></p><pre><code >import org.apache.commons.beanutils.BeanUtils;Object value = BeanUtils.getProperty(myObject, \"propertyName\");\n相比直接使用反射获取字段值，这种方式更加简洁且性能更优。通过这些优化策略，可以在享受反射带来的灵活性的同时，尽可能减少其对性能的负面影响 。</code></pre><p><br></p>','2025-03-05 10:18:38','2025-03-05 10:18:38',1,0,'从不同角度剖析java反射',0,0,0,0),(1897232625857466370,'Java 反射的丰富应用场景',1,'<p><br></p><p><br></p><h3>框架开发</h3><p>以 Spring 框架为例，其核心的依赖注入（DI）和面向切面编程（AOP）功能都离不开反射。在 DI 中，Spring 通过读取配置文件（如 XML 或注解），利用反射动态创建对象并注入依赖。例如，当配置了一个 Bean，Spring 会根据类名使用反射创建对象：</p><p><br></p><pre><code >// 假设配置的类名为com.example.MyServiceClass&lt;?&gt; clazz = Class.forName(\"com.example.MyService\");Object myService = clazz.newInstance();\n在 AOP 中，反射用于在运行时动态代理目标对象，织入切面逻辑，实现日志记录、事务管理等功能。\n</code></pre><h3>插件化架构</h3><p>许多大型应用希望具备插件化能力，允许用户在不修改核心代码的情况下添加新功能。通过反射，应用可以在运行时加载外部插件类。比如，一个绘图软件可能允许用户安装不同的绘图算法插件。应用读取插件配置文件，获取插件类名，然后使用反射加载并实例化插件类：</p><p><br></p><pre><code >String pluginClassName = readPluginConfig(\"plugin1\");Class&lt;?&gt; pluginClass = Class.forName(pluginClassName);Plugin plugin = (Plugin) pluginClass.newInstance();plugin.execute();\n</code></pre><h3>单元测试</h3><p>在单元测试中，有时需要测试私有方法或访问私有字段。反射可以突破访问限制，让测试代码能够调用私有方法、设置私有字段的值，从而全面测试类的功能。例如：</p><p><br></p><pre><code >// 测试私有方法Class&lt;?&gt; clazz = TargetClass.class;Method privateMethod = clazz.getDeclaredMethod(\"privateMethod\", parameterTypes);privateMethod.setAccessible(true);privateMethod.invoke(targetObject, args);// 测试私有字段Field privateField = clazz.getDeclaredField(\"privateField\");privateField.setAccessible(true);privateField.set(targetObject, value);\n此外，反射在注解处理、对象序列化与反序列化等场景中也有着广泛应用，它让 Java 程序在运行时的行为更加动态和可定制。</code></pre><p><br></p>','2025-03-05 10:28:02','2025-03-05 10:28:02',1,0,'Java 反射机制在众多实际场景中都发挥着关键作用，为开发者提供了强大的工具来构建更灵活、可扩展的应用。',0,0,0,0),(1897232938253422594,'Java 反射性能优化策略',1,'<p><br></p><h3>减少反射调用次数</h3><p>尽量避免在循环中频繁使用反射。例如，如果需要多次调用一个反射方法，考虑将反射获取的Method对象缓存起来，而不是每次都重新获取。</p><p><br></p><pre><code >// 错误示范：每次循环都获取Method对象for (int i = 0; i &lt; 1000; i++) {    Class&lt;?&gt; clazz = MyClass.class;    Method method = clazz.getMethod(\"methodName\");    method.invoke(myObject);}// 正确示范：缓存Method对象Class&lt;?&gt; clazz = MyClass.class;Method method = clazz.getMethod(\"methodName\");for (int i = 0; i &lt; 1000; i++) {    method.invoke(myObject);}\n</code></pre><h3>使用 JVM 反射优化机制</h3><p>JVM 自身对反射有一定的优化措施。当一个反射方法调用次数达到阈值（默认是 15 次）时，JVM 会自动对该调用进行优化，内部会转换为对某个类或实例方法的直接调用，从而提高效率。我们可以通过设置 JVM 属性来控制这一优化过程。例如，sun.reflect.noInflation属性可以控制是否直接生成动态类来优化反射调用，sun.reflect.inflationThreshold属性可以设置生成动态类的阈值。在 Idea 中，可以在运行配置的 VM options 中设置这些属性：</p><p><br></p><pre><code >-Dsun.reflect.noInflation=true-Dsun.reflect.inflationThreshold=10\n</code></pre><h3>使用反射工具类</h3><p>一些第三方反射工具类，如 Apache Commons BeanUtils，对反射操作进行了封装和优化。它们通常会缓存反射信息，减少重复的反射查找，从而提高性能。例如，使用 BeanUtils 获取对象属性值：</p><p><br></p><pre><code >import org.apache.commons.beanutils.BeanUtils;Object value = BeanUtils.getProperty(myObject, \"propertyName\");\n相比直接使用反射获取字段值，这种方式更加简洁且性能更优。通过这些优化策略，可以在享受反射带来的灵活性的同时，尽可能减少其对性能的负面影响 。</code></pre><p><br></p>','2025-03-05 10:29:16','2025-03-05 10:29:16',1,0,'Java 反射性能优化策略\n虽然 Java 反射机制功能强大，但它也存在性能开销的问题。由于反射操作涉及运行时的类型检查、动态创建对象和方法调用，相比直接的代码调用，性能会有明显下降。不过，我们可以采取一些策略来优化反射性能。',0,0,1,0);
/*!40000 ALTER TABLE `blog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `blog_comment`
--

DROP TABLE IF EXISTS `blog_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `blog_id` bigint NOT NULL COMMENT '博客id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `context` text NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `is_delete` int DEFAULT '0' COMMENT '是否删除 0：删除 1 未删除',
  `is_visible` int DEFAULT '0' COMMENT '是否可见 0：可见 1 不可见',
  `kudos` bigint DEFAULT '0' COMMENT '评论点赞数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1898669335464148995 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blog_comment`
--

LOCK TABLES `blog_comment` WRITE;
/*!40000 ALTER TABLE `blog_comment` DISABLE KEYS */;
INSERT INTO `blog_comment` VALUES (1895671313595727874,1893581843467694082,1,'我的评论','2025-03-01 03:03:56','2025-03-01 03:03:56',0,0,1),(1895673373321953281,1893581843467694082,1,'123123','2025-03-01 03:12:07','2025-03-01 03:12:07',0,0,0),(1895676661199110146,1893581843467694082,1,'123123','2025-03-01 03:25:11','2025-03-01 03:25:11',0,0,0),(1895715791526293505,1893581843467694082,1,'第二条评论','2025-03-01 06:00:40','2025-03-01 06:00:40',0,0,0),(1895718566507180034,1893581843467694082,1,'asfasdfasdfadfadsfasdfasdasdfasdfadsfadsfasdfasdfasdfasdfasdfasdasdfasdfas','2025-03-01 06:11:42','2025-03-01 06:11:42',0,0,0),(1895725341436407809,1893581843467694082,1,'abc','2025-03-01 06:38:37','2025-03-01 06:38:37',0,0,0),(1895725359174119425,1893581843467694082,1,'123123','2025-03-01 06:38:41','2025-03-01 06:38:41',0,0,0),(1895804036389904385,1893581843467694082,1,'wowowwowo','2025-03-01 11:51:19','2025-03-01 11:51:19',0,0,0),(1895804430688034817,1893581843467694082,1,'ssss','2025-03-01 11:52:53','2025-03-01 11:52:53',0,0,1),(1895804826525474818,1893581843467694082,1,'ababab','2025-03-01 11:54:28','2025-03-01 11:54:28',0,0,1),(1895805380433649666,1894939800474304514,1,'第一条评论','2025-03-01 11:56:40','2025-03-01 11:56:40',0,0,0),(1895805660462161921,1894939800474304514,1,'123123','2025-03-01 11:57:46','2025-03-01 11:57:46',0,0,1),(1896899597004292098,1896899273258549249,1,'写的真的不错','2025-03-04 12:24:41','2025-03-04 12:24:41',0,0,0),(1896899632626515970,1896899273258549249,1,'写的真的不错','2025-03-04 12:24:50','2025-03-04 12:24:50',0,0,0),(1896899754206806018,1896899273258549249,1,'123','2025-03-04 12:25:19','2025-03-04 12:25:19',0,0,0),(1898669335464148994,1893581843467694082,1893237050568912898,'写的真垃圾','2025-03-09 09:37:00','2025-03-09 09:37:00',0,0,1);
/*!40000 ALTER TABLE `blog_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reply_table`
--

DROP TABLE IF EXISTS `reply_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reply_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `comment_id` bigint NOT NULL COMMENT '评论id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `context` text NOT NULL COMMENT '评论内容',
  `kudos` bigint DEFAULT '0' COMMENT '评论点赞数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `is_delete` int DEFAULT '0' COMMENT '是否删除 0：删除 1 未删除',
  `is_visible` int DEFAULT '0' COMMENT '是否可见 0：可见 1 不可见',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1895804887791673347 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论回复表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reply_table`
--

LOCK TABLES `reply_table` WRITE;
/*!40000 ALTER TABLE `reply_table` DISABLE KEYS */;
INSERT INTO `reply_table` VALUES (1895733718916919298,1895671313595727874,1,'我的回复',0,'2025-03-01 07:11:54','2025-03-01 07:11:54',0,0),(1895762321041813505,1895671313595727874,1,'我的回复2',1,'2025-03-01 09:05:34','2025-03-01 09:05:34',0,0),(1895802905186115585,1895673373321953281,1,'abc',0,'2025-03-01 11:46:50','2025-03-01 11:46:50',0,0),(1895803276138749954,1895676661199110146,1,'我是你爹',0,'2025-03-01 11:48:18','2025-03-01 11:48:18',0,0),(1895804887791673346,1895804826525474818,1,'123123123123asdfasdfasdfasdf',0,'2025-03-01 11:54:42','2025-03-01 11:54:42',0,0);
/*!40000 ALTER TABLE `reply_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `type_blog`
--

DROP TABLE IF EXISTS `type_blog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `type_blog` (
  `type_id` bigint NOT NULL COMMENT '类型id',
  `blog_id` bigint NOT NULL COMMENT '博客id',
  PRIMARY KEY (`type_id`,`blog_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `type_blog`
--

LOCK TABLES `type_blog` WRITE;
/*!40000 ALTER TABLE `type_blog` DISABLE KEYS */;
INSERT INTO `type_blog` VALUES (1,1893581843467694082),(1,1893845854478217218),(1,1893848517571850242),(1,1893848556310441986),(1,1894939800474304514),(1,1896899273258549249),(1,1897126754930515970),(1,1897213872725823490),(1,1897215538200055810),(1,1897215757268553729),(1,1897217720592896001),(1,1897221387832266754),(1,1897224059738132481),(1,1897225375055089666),(1,1897226669299863553),(1,1897228415841603585),(1,1897230261939343361),(1,1897232625857466370),(1,1897232938253422594),(2,1894939800474304514),(3,1897126754930515970),(3,1897230261939343361),(3,1897232625857466370),(3,1897232938253422594),(4,1893581843467694082),(10,1893581843467694082),(10,1893845854478217218),(12,1893581843467694082),(12,1894939800474304514),(12,1896899273258549249);
/*!40000 ALTER TABLE `type_blog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `type_table`
--

DROP TABLE IF EXISTS `type_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `type_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `type_table`
--

LOCK TABLES `type_table` WRITE;
/*!40000 ALTER TABLE `type_table` DISABLE KEYS */;
INSERT INTO `type_table` VALUES (1,'AFTER_END'),(2,'BEFORE_END'),(3,'JAVA'),(4,'C11'),(5,'C'),(6,'PYTHON'),(7,'GOLANG'),(8,'VUE'),(9,'HTML'),(10,'CSS'),(11,'JAVASCRIPT'),(12,'COMPUTER'),(13,'OS');
/*!40000 ALTER TABLE `type_table` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-09 17:52:16
