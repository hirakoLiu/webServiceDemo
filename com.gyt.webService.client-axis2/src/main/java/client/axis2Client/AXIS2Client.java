/**
 * FileName: AXIS2Client
 * Author:   Rock_Guo
 * Date:     2018/6/20 16:58
 * Description: AXIS2客户端
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package client.axis2Client;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.rpc.client.RPCServiceClient;

import javax.xml.namespace.QName;


/**
 * 〈一句话功能简述〉<br> 
 * 〈AXIS2客户端〉
 *
 * @author Rock_Guo
 * @create 2018/6/20
 * @since 1.0.0
 */
public class AXIS2Client {


    /**
     * 方法一：
     * 应用rpc的方式调用 这种方式就等于远程调用，
     * 即通过url定位告诉远程服务器，告知方法名称，参数等， 调用远程服务，得到结果。
     * 使用 org.apache.axis2.rpc.client.RPCServiceClient类调用WebService
     *
     【注】：

     如果被调用的WebService方法有返回值 应使用 invokeBlocking 方法 该方法有三个参数
     第一个参数的类型是QName对象，表示要调用的方法名；
     第二个参数表示要调用的WebService方法的参数值，参数类型为Object[]；
     当方法没有参数时，invokeBlocking方法的第二个参数值不能是null，而要使用new Object[]{}。
     第三个参数表示WebService方法的 返回值类型的Class对象，参数类型为Class[]。


     如果被调用的WebService方法没有返回值 应使用 invokeRobust 方法
     该方法只有两个参数，它们的含义与invokeBlocking方法的前两个参数的含义相同。

     在创建QName对象时，QName类的构造方法的第一个参数表示WSDL文件的命名空间名，
     也就是 <wsdl:definitions>元素的targetNamespace属性值。
     *
     */
    public void callService(){
        try {
            String url = "http://localhost:8080/webService/cxf/cxf-service?wsdl";

            // 使用RPC方式调用WebService
            RPCServiceClient serviceClient = new RPCServiceClient();
            // 指定调用WebService的URL
            EndpointReference targetEPR = new EndpointReference(url);
            Options options = serviceClient.getOptions();
            //确定目标服务地址
            options.setTo(targetEPR);
            //确定调用方法（注意：使用此方法，服务端代码中，接口方法必须加上注解 @WebMethod(action="callService") ）
            options.setAction("callService");

            String namespace = "http://service.demo/";
            addValidation(serviceClient, namespace, "puan.zhangsan", "123456");

            /**
             * 指定要调用的callService方法及WSDL文件的命名空间
             * 如果 webservice 服务端由axis2编写
             * 命名空间 不一致导致的问题
             * org.apache.axis2.AxisFault: java.lang.RuntimeException: Unexpected subelement arg0
             */
            QName qname = new QName(namespace, "callService");
            // 指定callService方法的参数值
            Object[] parameters = new Object[] { "Hi Service, I'm AXIS2Client !" };

            // 指定callService方法返回值的数据类型的Class对象
            Class[] returnTypes = new Class[] { String.class };

            // 调用方法一 传递参数，调用服务，获取服务返回结果集
            OMElement element = serviceClient.invokeBlocking(qname, parameters);
            //值得注意的是，返回结果就是一段由OMElement对象封装的xml字符串。
            //我们可以对之灵活应用,下面我取第一个元素值，并打印之。因为调用的方法返回一个结果
            String result = element.getFirstElement().getText();
            System.out.println(result);

            // 调用方法二 callService方法并输出该方法的返回值
            Object[] response = serviceClient.invokeBlocking(qname, parameters, returnTypes);
             String r = (String) response[0];
            System.out.println(r);

        }catch (Exception e){
            System.out.println("AXIS2Client Exception: "+e.getMessage());
        }

    }


    /**
     * 方法二： 应用document方式调用
     * 用ducument方式应用现对繁琐而灵活。现在用的比较多。因为真正摆脱了我们不想要的耦合
     */
    public void callService2() {
        try {
            String url = "http://localhost:8080/webService/cxf/cxf-service?wsdl";

            Options options = new Options();
            // 指定调用WebService的URL
            EndpointReference targetEPR = new EndpointReference(url);
            options.setTo(targetEPR);

            ServiceClient sender = new ServiceClient();
            sender.setOptions(options);

            OMFactory fac = OMAbstractFactory.getOMFactory();
            String tns = "http://service.demo/";
            // 命名空间，有时命名空间不增加没事，不过最好加上，因为有时有事，你懂的
            OMNamespace omNs = fac.createOMNamespace(tns, "");

            OMElement method = fac.createOMElement("callService", omNs);

            // 添加参数 （注意：接口中要对参数进行注解  @WebParam(name="arg0", targetNamespace="http://service.demo/") ）
            // 否则会报错  org.apache.axis2.AxisFault: Unmarshalling Error: 意外的元素 (uri:"http://service.demo/", local:"arg0")。所需元素为<{}arg0>
            OMElement arg0 = fac.createOMElement("arg0", omNs);

            arg0.addChild(fac.createOMText(arg0, " Hi Service, I'm AXIS2Client, method2 ! "));
            method.addChild(arg0);
            method.build();

            addValidation(sender, tns, "puan.zhangsan", "123456");
            OMElement result = sender.sendReceive(method);

            String response = result.getFirstElement().getText();
            System.out.println(response);

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }

    /**
     * 为SOAP Header构造验证信息，
     * 如果你的服务端是没有验证的，那么你不用在Header中增加验证信息
     *
     * @param serviceClient
     * @param tns 命名空间
     * @param user
     * @param passwrod
     */
    private void addValidation(ServiceClient serviceClient, String tns , String user, String passwrod) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(tns, "");
        OMElement header = fac.createOMElement("AuthToken", omNs);
        OMElement ome_user = fac.createOMElement("username", omNs);
        OMElement ome_pass = fac.createOMElement("password", omNs);

        ome_user.setText(user);
        ome_pass.setText(passwrod);

        header.addChild(ome_user);
        header.addChild(ome_pass);

        serviceClient.addHeader(header);
    }

}
