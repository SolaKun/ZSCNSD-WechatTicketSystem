package love.sola.netsupport.auth;

import love.sola.netsupport.session.WechatSession;
import love.sola.netsupport.session.WxSession;
import love.sola.netsupport.util.Checker;
import love.sola.netsupport.wechat.WxMpServlet;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ***********************************************
 * Created by Sola on 2014/8/20.
 * Don't modify this source without my agreement
 * ***********************************************
 */
@WebServlet(name = "OAuth2", urlPatterns = "/oauth2/callback", loadOnStartup = 21, asyncSupported = true)
public class OAuth2 extends HttpServlet {

	private static Map<String, OAuth2Handler> oAuth2HandlerMap = new HashMap<>();

	/**
	 * for {@link love.sola.netsupport.wechat.WxMpServlet#registerCommands}
	 * @param state the state key from open platform callback.
	 * @param handler handler
	 */
	public static void registerOAuth2Handler(String state, OAuth2Handler handler) {
		oAuth2HandlerMap.put(state, handler);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		AsyncContext acxt = req.startAsync();
		String code = req.getParameter("code");
		String state = req.getParameter("state");
		if (Checker.hasNull(code, state)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		OAuth2Handler handler = oAuth2HandlerMap.get(state);
		if (handler == null) {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
			return;
		}
		acxt.start(() -> {
			try {
				WxMpService wxMpService = WxMpServlet.instance.wxMpService;
				WxMpOAuth2AccessToken token = wxMpService.oauth2getAccessToken(code);
				String wechat = token.getOpenId();
				WxSession session = WechatSession.create();
				handler.onOAuth2(acxt, (HttpServletResponse) acxt.getResponse(), wechat, session);
				acxt.complete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

}
