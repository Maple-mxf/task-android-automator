//package one.rewind.android.automator;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//import org.springframework.web.servlet.view.InternalResourceViewResolver;
//
//@SpringBootApplication
//@Configuration
//@EnableWebMvc
//@Controller
//@RequestMapping(value = "/")
//public class ViewControlApplication {
//
//    @Bean
//    public InternalResourceViewResolver viewResolver() {
//
//        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
//
//        viewResolver.setPrefix("/templates/");
//
//        viewResolver.setSuffix(".jsp");
//
//        return viewResolver;
//    }
//
//    public static void main(String[] args) {
//        SpringApplication.run(ViewControlApplication.class);
//    }
//
//    @RequestMapping(value = "index")
//    public String index() {
//        System.out.println("hello world");
//        return "index";
//    }
//
//    @RequestMapping(value = "to")
//    @ResponseBody
//    public Object msg() {
//        return "Hello World";
//    }
//}
