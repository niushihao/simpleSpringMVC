package com.nsh.myServlet;

import com.nsh.ann.Controller;
import com.nsh.ann.RequestMapping;
import com.nsh.ann.Resource;
import com.nsh.ann.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nsh on 2017/7/18.
 */
public class DispatcherServlet extends HttpServlet{

    private static final long serialVersionUID =1L;

    //初始化bean ,将初始化后的bean放入容器
    private List<String> packageNames =new ArrayList<String>();

    //类实例
    private Map<String,Object> instanceMaps = new HashMap<String, Object>();

    //处理请求的springmvc方法对象
    private Map<String,Method> handerMap = new HashMap<String, Method>();
    /**
     * 初始化核心控制器
     * @throws ServletException
     */
    public void init() {
        try {
            //加载要扫描的基包，也可以通过xml配置
            String basePackage ="com.nsh";
            //扫描包中的bean，并加入spring容器
            scanBasePackage(basePackage);
            //找到实例，通过全包类名找到实例
            filterAndInstance();
            //依赖注入
            springIoc();
            //通过接受的url请求判断由哪个控制器处理（hander）
            handlerMaps();
        }catch (Exception e){

        }

    }

    //通过映射方法链找到对用的handler类
    private void handlerMaps() {
        if(instanceMaps.size()<=0){
            return;
        }
        //有控制层和service层实例
        for(Map.Entry<String,Object> entry:instanceMaps.entrySet()){
            if(entry.getValue().getClass().isAnnotationPresent(Controller.class)){
                Controller controller =(Controller)entry.getValue().getClass().getAnnotation(Controller.class);
                String baseUrl =controller.value();
                Method[] controllerMethods =entry.getValue().getClass().getMethods();
                for(Method controllerMethod:controllerMethods){
                    if(controllerMethod.isAnnotationPresent(RequestMapping.class)){
                        String methodUrl =((RequestMapping)(controllerMethod.getAnnotation(RequestMapping.class))).value();
                        handerMap.put("/"+baseUrl+"/"+methodUrl,controllerMethod);
                    }else{
                        continue;
                    }
                }
            }

        }
    }

    private void springIoc() throws IllegalAccessException {
        if(instanceMaps.size()<=0){
            return;
        }
        for (Map.Entry<String,Object> entry:instanceMaps.entrySet()){
            Field[] fields =entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
                if(field.isAnnotationPresent(Resource.class)){
                    Resource resource =(Resource)field.getAnnotation(Resource.class);
                    String key =resource.value();
                    field.setAccessible(true);
                    field.set(entry.getValue(),instanceMaps.get(key));
                }else {
                    continue;
                }
            }

        }
    }

    private void filterAndInstance() throws Exception{
        if(packageNames.size()<=0){
            return;
        }

        for (String className:packageNames) {
            Class clazz =Class.forName(className.replace(".class",""));
            if(clazz.isAnnotationPresent(Controller.class)){
                //得到控制层的实例
                Object instance =clazz.newInstance();

                Controller controller = (Controller) clazz.getAnnotation(Controller.class);
                //获取注解的值
                String key =controller.value();
                //装入实例列表
                instanceMaps.put(key,instance);
            }else if(clazz.isAnnotationPresent(Service.class)){
                //得到业务的实例
                Object instance =clazz.newInstance();

                Service controller = (Service) clazz.getAnnotation(Service.class);
                //获取注解的值
                String key =controller.value();
                //装入实例列表
                instanceMaps.put(key,instance);
            }else{
                continue;
            }
        }

    }

    /**
     * 全包扫描
     * @param basePackage
     */
    private void scanBasePackage(String basePackage) {

       URL url = this.getClass().getClassLoader().getResource("/"+replace(basePackage));
       //注意这个文件下有文件也有文件夹
       String pathFile =url.getFile();
       File file= new File(pathFile);
       String[] files =file.list();
       for(String path:files){
           File eachFile =new File(pathFile+path);
           //如果是文件夹
           if(eachFile.isDirectory()){
               scanBasePackage(basePackage+"."+eachFile.getName());
           }else if(eachFile.isFile()){
               System.out.println("simple spring容器加载到的类："+basePackage+"."+eachFile.getName());
               //将我们扫描到的全包类名加入容器
               packageNames.add(basePackage+"."+eachFile.getName());
           }
       }
    }

    /**
     * 将com.nsh 换成 com/nsh,方便File来操作
     * @param basePackage
     * @return path
     */
    private String replace(String basePackage) {
        return basePackage.replace("\\.","/");
    }
}
