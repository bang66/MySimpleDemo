package IdeNetty.Handler;

import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MyEmail {

    private static String EMAIL_FROM = "lzh1229879081@163.com";
    private static String PASSWD = "lzh122";
    private static String HOST = "smtp.163.com";
    private static String EMAIL_TO;
    private boolean flag = false;
    private static final Logger logger = Logger.getLogger(MyEmail.class);

    public MyEmail(String email_to) {
        this.EMAIL_TO = email_to;
        logger.info("Send email to " + this.EMAIL_TO);
    }

    public String sendMsg() {
        logger.info("开始配置邮件....");
        //创建邮件配置文件
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.transport.protocol", "smtp");
        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", HOST);
//        properties.setProperty("mail.smtp.port","25");
        properties.put("mail.smtp.auth", "true"); // 通过验证

        properties.put("mail.smtp.ssl.enable", "true");//开启ssl
        // 获取默认session对象
        Session session = Session.getInstance(properties);
        session.setDebug(false);
        Transport ts = null;
        try {
            ts = session.getTransport();
        } catch (NoSuchProviderException e) {
            logger.error("session.getTransport() 方法", e);
            return "{\"status\":\"fail\"}";
        }
        try {
            ts.connect(EMAIL_FROM, PASSWD);
        } catch (MessagingException e) {
            logger.error("连接出错", e);
            return "{\"status\":\"fail\"}";
        }
        Message message = null;
        try {
            message = createSimpleMail(session);
            ts.sendMessage(message, message.getAllRecipients());
        } catch (MessagingException e) {
            logger.error("sendMess error", e);
            return "{\"status\":\"fail\"}";
        }
        logger.info("email send successful");
        flag = true;
        return "{\"status\":\"ok\"}";
    }

    public MimeMessage createSimpleMail(Session session) throws MessagingException {
        //创建邮件对象
        MimeMessage mm = new MimeMessage(session);
        //设置发件人
        mm.setFrom(new InternetAddress(EMAIL_FROM));
        //设置收件人
        mm.addRecipients(Message.RecipientType.CC, InternetAddress.parse(EMAIL_FROM));
        mm.setRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_TO));
        mm.setSubject("Serverless服务邮箱注册");
        mm.setContent("<a href=\"http://localhost:8088/?email=" + EMAIL_TO + "&flag=register\">请点击连接进行注册</a>", "text/html;charset=utf-8");
        logger.info("邮件配置成功");
        return mm;
    }

    public boolean getflag() {
        return this.flag;
    }
}
