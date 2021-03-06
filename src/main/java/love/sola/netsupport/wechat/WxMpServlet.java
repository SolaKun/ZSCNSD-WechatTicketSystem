/*
 * This file is part of WechatTicketSystem.
 *
 * WechatTicketSystem is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WechatTicketSystem is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WechatTicketSystem.  If not, see <http://www.gnu.org/licenses/>.
 */

package love.sola.netsupport.wechat;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import love.sola.netsupport.auth.OAuth2;
import love.sola.netsupport.auth.OAuth2Handler;
import love.sola.netsupport.config.Settings;
import love.sola.netsupport.sql.SQLCore;
import love.sola.netsupport.wechat.handler.RegisterHandler;
import love.sola.netsupport.wechat.handler.SubscribeHandler;
import love.sola.netsupport.wechat.matcher.CheckSpamMatcher;
import love.sola.netsupport.wechat.matcher.RegisterMatcher;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.util.StringUtils;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;

import static love.sola.netsupport.config.Lang.lang;

/**
 * @author Sola {@literal <dev@sola.love>}
 */
@WebServlet(name = "WxMpServlet", urlPatterns = "/wechat", loadOnStartup = 99)
public class WxMpServlet extends HttpServlet {

    public static WxMpServlet instance;
    protected WxMpInMemoryConfigStorage config;
    public WxMpService wxMpService;
    protected WxMpMessageRouter wxMpMessageRouter;
    protected CheckSpamMatcher checkSpamMatcher;

    public WxMpServlet() {
        instance = this;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        config = new WxMpInMemoryConfigStorage();
        config.setAppId(Settings.I.Wechat_AppId);
        config.setSecret(Settings.I.Wechat_Secret);
        config.setToken(Settings.I.Wechat_Token);
        config.setAesKey(Settings.I.Wechat_AesKey);

        wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(config);

        checkSpamMatcher = new CheckSpamMatcher();
        wxMpMessageRouter = new WxMpMessageRouter(wxMpService);
        wxMpMessageRouter.rule()
                .async(false)
                .msgType(WxConsts.XML_MSG_EVENT)
                .event(WxConsts.EVT_SUBSCRIBE)
                .handler(new SubscribeHandler())
                .end();
        wxMpMessageRouter.rule()
                .async(false)
                .matcher(new CheckSpamMatcher())
                .handler((wxMessage, context, wxMpService1, sessionManager)
                        -> WxMpXmlOutMessage.TEXT()
                        .fromUser(wxMessage.getToUserName())
                        .toUser(wxMessage.getFromUserName())
                        .content(lang("Message_Spam")).build())
                .end();
        wxMpMessageRouter.rule()
                .async(false)
                .matcher(new RegisterMatcher())
                .handler(new RegisterHandler())
                .end();
        try {
            registerCommands(wxMpMessageRouter);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new ServletException(e);
        }
    }

    public static void registerCommands(WxMpMessageRouter router) throws IllegalAccessException, InstantiationException {
        for (Command c : Command.values()) {
            WxMpMessageHandler handler = c.handler.newInstance();
            router.rule().async(false).msgType(WxConsts.XML_MSG_TEXT).rContent(c.regex).handler(handler).end();
            router.rule().async(false).msgType(WxConsts.XML_MSG_EVENT).event(WxConsts.EVT_CLICK).eventKey(c.name()).handler(handler).end();
            if (handler instanceof OAuth2Handler) {
                OAuth2.registerOAuth2Handler(c.name(), (OAuth2Handler) handler);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String signature = request.getParameter("signature");
        String nonce = request.getParameter("nonce");
        String timestamp = request.getParameter("timestamp");

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            // Signature fail
            response.getWriter().println(lang("Access_Denied"));
            return;
        }

        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotBlank(echostr)) {
            // validate request
            response.getWriter().println(echostr);
            return;
        }

        String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ? "raw" : request.getParameter("encrypt_type");

//		if ("raw".equals(encryptType)) {
//			WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(request.getInputStream());
//			WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
//			if (outMessage == null) {
//				outMessage = WxMpXmlOutMessage.TEXT()
//						.fromUser(inMessage.getToUserName())
//						.toUser(inMessage.getFromUserName())
//						.content(lang("Invalid_Operation"))
//						.build();
//			}
//			response.getWriter().write(outMessage.toXml());
//			return;
//		}

        if ("aes".equals(encryptType)) {
            String msgSignature = request.getParameter("msg_signature");
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(request.getInputStream(), config, timestamp, nonce, msgSignature);
            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            if (outMessage == null) {
                outMessage = WxMpXmlOutMessage.TEXT()
                        .fromUser(inMessage.getToUserName())
                        .toUser(inMessage.getFromUserName())
                        .content(lang("Invalid_Operation"))
                        .build();
            }
            response.getWriter().write(outMessage.toEncryptedXml(config));
            return;
        }

        response.getWriter().println(lang("Unknown_Encrypt_Type"));
        return;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    public void destroy() {
        SQLCore.destroy();
    }
}
