# 写在开头

- github: [[markyao98/mkyao-rpc (github.com)]()](https://github.com/markyao98/mkyao-rpc)

- 本框架前身:[micro-rpc: 本项目为仿照dubbo开发的rpc框架。 (gitee.com)](https://gitee.com/markYao98/micro-rpc)
- gitee版，不集成spring，而且也没有实战使用过，但是里面除了tcp协议还集成了http协议，用来了解rpc的原理还是挺清晰的，属于学习版本。

- 本版本集成了Spring，修复了一些使用过程中可能产生的bug，并且做了一定的优化，提升了tps；同时添加了服务降级的功能，与之匹配的注册中心也做了进一步的开发。



# 如何使用
![1691845410363](https://github.com/markyao98/mkyao-rpc/assets/87574222/a278c591-5e7e-447e-94b0-5043964ff9f4)


- 本框架使用了redis做服务注册中心，所以需要你启动redis服务，并且在项目中写上相关配置。默认情况下，我这里redis服务使用了9527端口。

![1691844621906](https://github.com/markyao98/mkyao-rpc/assets/87574222/0eb3832b-aa46-47f8-ad30-d43394fbe376)


- 需要在resources下添加dubbo-application.xml配置文件（注意名字要相同）
- 配置内容

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<dubbo>

    <application>
        <name>test-consumer</name>
        <host>127.0.0.1</host>
        <port>18002</port>
        <weight>1</weight>
    </application>

</dubbo>
```

- 启动你的redis即可。

- 我已经写了一个example项目，可以直接参考里面的用法，由于是基于注解，所以还是比较容易上手的。
- 注意： 需要把jar包引入全局库，或者直接拷贝源码项目过去。

- 项目结构: 

![1691844712738](https://github.com/markyao98/mkyao-rpc/assets/87574222/e1aa353c-0b21-44dc-ae72-f40826200519)


- lib目录下存放的是框架的jar包
- commons是公共接口
- 其他两个顾名思义，消费者与生产者

## 服务提供者

```java
/**
 * Springboot启动类，固定写法.此处是为了启动rpc服务,记得开启本地redis9527端口
 */
@SpringBootApplication
public class Provider1Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Provider1Application.class, args);
        ConfigurableListableBeanFactory configurableListableBeanFactory =null;
        while (configurableListableBeanFactory==null){
            sleep(100);
            configurableListableBeanFactory=run.getBeanFactory();
        }
        SpringDubboApplicationContext context=
                new SpringDubboApplicationContext(configurableListableBeanFactory, ProtocolTypes.DUBBO, LoadBalance.RANDAM_WEIGHT);
        context.refresh();
    }

    @PreDestroy
    public void destroy(){
        RedisRegistry.clearAll();
    }
    private static void sleep(int times) {
        try {
            Thread.sleep(times);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


/**
 * 实现公共服务接口 UserService
 * 加上以下两个注解 @ServiceProvider,@Component
 */
@ServiceProvider
@Component
public class RpcUserService implements UserService{
    @Override
    public List<User> getUsers() {
        return TestDataGenarator.genaratorUsers();
    }

    @Override
    public User getUser(int id) {
        return TestDataGenarator.getUser(id);
    }

    @Override
    public String testError(String msg) {
        System.out.println("userservice 消息:"+msg);
        int i=10/0;
        return "i=10/0";
    }
}


/**
 * 实现公共服务接口 UserService
 * 加上以下两个注解 @ServiceProvider,@Component
 */
@ServiceProvider
@Component
public class RpcBookService implements BookService{

    @Override
    public List<Book> getBooks() {
        return TestDataGenarator.getBooks();
    }

    @Override
    public Book getBook(Integer id) {
        return TestDataGenarator.getBook(id);
    }

    /**
     * 测试服务出错
     * @param msg
     * @return
     */
    @Override
    public String testError(String msg) {
        System.out.println("bookservice 消息:"+msg);
        int i=10/0;
        return "i=10/0";
    }
}
```

## 服务消费者

```java
@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(ConsumerApplication.class, args);
        ConfigurableListableBeanFactory configurableListableBeanFactory = run.getBeanFactory();
        SpringDubboApplicationContext context=
                new SpringDubboApplicationContext(configurableListableBeanFactory, ProtocolTypes.DUBBO, LoadBalance.RANDAM_WEIGHT);
        context.refresh();
    }

    @PreDestroy
    public void destroy(){
        RedisRegistry.clearAll();
    }
}

@RestController
public class UserController {
    /**
     * 直接在这里引入userservice即可调用服务，使用ServiceRefrence注解
     * 服务出错策略: 1.重试 2.服务降级，执行fallback方法
     * 这里演示服务降级,需要指定降级fallback的class类
     */
    @ServiceRefrence(demotePolicy = DemotePolicy.DEMOTE_FALLBACK,fallback = UserServiceFallback.class)
    UserService rpcUserService;

    @GetMapping("/users/list")
    public RestData userList(){
        List<User> users = rpcUserService.getUsers();
        return RestData.success(users);
    }
    @GetMapping("/user/{id}")
    public RestData getUser(@PathVariable("id") Integer id){
        User user = rpcUserService.getUser(id);
        return RestData.success(user);
    }
    @GetMapping("/user/testError/{msg}")
    public RestData testError(@PathVariable("msg") String msg){
        String s = rpcUserService.testError(msg);
        return RestData.success(s);
    }

}

@RestController
public class BookController {
    /**
     * 直接在这里引入bookservice即可调用服务，使用ServiceRefrence注解
     * 服务出错策略: 1.重试 2.服务降级，执行fallback方法
     * 这里演示重试,默认重试3次，可以指定重试次数
     */
    @ServiceRefrence(demotePolicy = DemotePolicy.RETRY,retrycnt = 3)
    BookService rpcBookService;

    @GetMapping("/books/list")
    public RestData bookList(){
        List<Book> books = rpcBookService.getBooks();
        return RestData.success(books);
    }
    @GetMapping("/book/{id}")
    public RestData book(@PathVariable("id") Integer id){
        Book book = rpcBookService.getBook(id);

        return RestData.success(book);
    }
    @GetMapping("/book/testError/{msg}")
    public RestData testError(@PathVariable("msg") String msg){
        String s = rpcBookService.testError(msg);
        return RestData.success(s);
    }


}

/**
 * 服务降级类,需要实现对应服务接口
 */
public class UserServiceFallback implements UserService {
    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public User getUser(int id) {
        return null;
    }

    /**
     * 在这里测试报错方法的fallback执行
     * @param msg
     * @return
     */
    @Override
    public String testError(String msg) {
        System.out.println("报错方法的fallback执行");
        return "报错方法的fallback执行,接收到消息: "+msg;
    }
}


```



## 接口测试效果

![1691839667039](https://github.com/markyao98/mkyao-rpc/assets/87574222/4151346d-b73b-458b-8a00-40a85c551475)




![1691839674402](https://github.com/markyao98/mkyao-rpc/assets/87574222/42dcf8dc-c120-44b5-9ecb-0709e22dfb8f)




![1691839684168](https://github.com/markyao98/mkyao-rpc/assets/87574222/0f9edc3e-b834-4cd2-ab9c-11d091c319d2)


![1691839697961](https://github.com/markyao98/mkyao-rpc/assets/87574222/5833832b-e56a-4674-a54b-d2c896070912)




- user服务

![1691842852869](https://github.com/markyao98/mkyao-rpc/assets/87574222/d63fe19c-c87d-4602-8233-2902809c150f)




![1691842860511](https://github.com/markyao98/mkyao-rpc/assets/87574222/6aa34f37-09dc-4080-824d-dc4893a4e7c9)






![1691842883685](https://github.com/markyao98/mkyao-rpc/assets/87574222/6309935a-f0b6-4bfa-b16b-d09e286e44df)


![1691842893393](https://github.com/markyao98/mkyao-rpc/assets/87574222/2486a964-601d-4cda-872c-ba8dae60b9c3)






# Jmeter压测(与dubbo对比)

- 选择测试接口： [localhost:8002/book/1](http://localhost:8002/book/1)

![1691843435225](https://github.com/markyao98/mkyao-rpc/assets/87574222/c7ecd47b-85f0-4e1e-952f-791558b6b9f6)






- Jmeter配置

![1691843571766](https://github.com/markyao98/mkyao-rpc/assets/87574222/1b0ccb64-5ae1-465d-83b0-6ebaf75054e8)


![1691843599693](https://github.com/markyao98/mkyao-rpc/assets/87574222/07ca9336-b528-4107-b3ce-a70f4e7d812c)


- 1000个线程，循环10次，总共1w次请求

- 结果如下：

![1691843698232](https://github.com/markyao98/mkyao-rpc/assets/87574222/83cbc64d-c974-4288-a91f-1e67f5bd5428)


- TPS： 7555(五次测试取最高值)



- 自取欺辱一下，挑战一波dubbo

- 我用dubbo平替自己的rpc，用同样的接口，同样的jemeter配置，测试一下。


![1691844366637](https://github.com/markyao98/mkyao-rpc/assets/87574222/b92d8426-3dc7-4e80-a32a-9742b130782b)


- TPS: 7468(五次测试取最高值)

- 虽然TPS达到了跟dubbo一样的水平，但是dubbo框架的完善性是我所无法对比的，特别是序列化这一块。
- 虚心学习，再接再励。
