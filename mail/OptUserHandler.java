package IdeNetty.Handler;

import IdeNetty.Db.Opt.OptUser;
import IdeNetty.Db.dao.UserDao;
import IdeNetty.Jedis.OptUserJedis;
import IdeNetty.MyHttpDecoder.MyHttpPostDecoder;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.log4j.Logger;

import java.util.HashMap;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OptUserHandler extends ChannelInboundHandlerAdapter {
    private static String EMAIL_TO;
    private static String USER_COUNT = "count";
    private static String USER_PASS = "pass";
    private static String USER_EMAIL = "email";
    private String response_json;
    private MyEmail myEmail;
    private static final Logger logger = Logger.getLogger(OptUserHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //此handler只处理登录与注册业务,若不是则flush
        FullHttpRequest request = (FullHttpRequest) msg;
        String request_url = request.uri();
        logger.info(request_url);
        System.out.println(request_url);
        if (!request_url.equals("/favicon.ico")) {
            if (request.method() == HttpMethod.POST) {
                if (request_url.equals("/register")) {
                    logger.info("开始进行用户url: " + request_url);
                    //开始进行注册,进行http解码获取user信息,并发送邮件进行验证
                    MyHttpPostDecoder myHttpPostDecoder = new MyHttpPostDecoder(request);
                    HashMap<String, String> paraMap = myHttpPostDecoder.httpPostDecode();
                    System.out.println(paraMap);
                    EMAIL_TO = (String) paraMap.get(USER_EMAIL);
                    System.out.println(EMAIL_TO);
                    //redis缓存用户注册信息数据,并设置k失效
                    UserDao user = new UserDao();
                    user.setCount((String) paraMap.get(USER_COUNT));
                    user.setEmail((String) paraMap.get(USER_EMAIL));
                    user.setPasswd((String) paraMap.get(USER_PASS));
                    OptUserJedis optUser = new OptUserJedis();
                    optUser.setUserDao(user);
                    optUser.saveMsg();
                    myEmail = new MyEmail(EMAIL_TO);
                    response_json = myEmail.sendMsg();
                    //发送email成功之后,向服务器返回response响应
                    HttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(response_json.getBytes("utf-8")));
                    response.headers().set(CONTENT_TYPE, "*/*");
                    response.headers().set("Access-Control-Allow-Origin", "*");
                    response.headers().setInt(CONTENT_LENGTH, ((DefaultFullHttpResponse) response).content().readableBytes());
                    logger.info("第一次验证完成");
                    ctx.writeAndFlush(response);
                } else if (request_url.contains("/login")) {
                    //开始进行登录比对
                    logger.info("判断为登录请求,url" + request_url);
                    MyHttpPostDecoder myDecoder = new MyHttpPostDecoder(request);
                    logger.info(myDecoder);
                    HashMap<String, String> user_msg = myDecoder.httpPostDecode();
                    System.out.println(user_msg);
                    UserDao userDao = new UserDao();
                    userDao.setEmail(user_msg.get("email"));
                    userDao.setPasswd(user_msg.get("pass"));
                    userDao.setCount(user_msg.get("count"));
                    System.out.println(userDao.toString());
                    OptUserJedis optUser = new OptUserJedis();
                    boolean flag = optUser.login(userDao);
                    String responseMsg;
                    if (flag) {
                        responseMsg = "{\"status\":\"ok\"}";
                    } else {
                        responseMsg = "{\"status\":\"fail\"}";
                    }
                    HttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(responseMsg.getBytes("utf-8")));
                    response.headers().set(CONTENT_TYPE, "*/*");
                    response.headers().set("Access-Control-Allow-Origin", "*");
                    response.headers().setInt(CONTENT_LENGTH, ((DefaultFullHttpResponse) response).content().readableBytes());
                    logger.info("登录请求完成");
                    ctx.writeAndFlush(response);
                } else {
                    //进入下一个handler
                    System.out.println();
                    ctx.fireChannelRead(msg);
                }
            } else if (request_url.contains("flag")) {
                //说明是从邮箱发过来的get请求,再判断email在不在redis中,若有则落库
                logger.info("判断为第二次进行验证,url: " + request_url);
                System.out.println(request_url);
                String[] messages = request_url.split("\\?");
                String result_url = messages[0];
                String email = messages[1].split("&")[0].split("=")[1];
                String flag = messages[1].split("&")[1].split("=")[1];
                System.out.println(result_url + "====>" + email + "===>" + flag);
                OptUserJedis optUserJedis = new OptUserJedis();
                boolean is_complite = optUserJedis.checkUser(email);
                String responseMsg;
                if (is_complite) {
                    responseMsg = "验证成功,请去登录";
//                    responseMsg = "{\"status\":\"ok\"}";
                } else {
                    responseMsg = "验证失败,请重新验证";
//                    responseMsg = "{\"status\":\"fail\"}";
                }
                HttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(responseMsg.getBytes("utf-8")));
                response.headers().set(CONTENT_TYPE, "*/*");
                response.headers().set("Access-Control-Allow-Origin", "*");
                response.headers().setInt(CONTENT_LENGTH, ((DefaultFullHttpResponse) response).content().readableBytes());
                logger.info("第二次验证完成");
                ctx.writeAndFlush(response);
            }
        }
    }
}
