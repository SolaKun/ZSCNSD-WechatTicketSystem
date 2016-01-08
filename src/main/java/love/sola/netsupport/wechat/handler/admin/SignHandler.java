package love.sola.netsupport.wechat.handler.admin;

import love.sola.netsupport.sql.SQLCore;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.outxmlbuilder.TextBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ***********************************************
 * Created by Sola on 2015/12/26.
 * Don't modify this source without my agreement
 * ***********************************************
 */
public class SignHandler implements WxMpMessageHandler {

	public static Pattern pat = Pattern.compile("(?i)^Auth (\\d{4})");

	@Override
	public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {
		String msg = wxMessage.getContent();
		TextBuilder out = WxMpXmlOutMessage.TEXT().toUser(wxMessage.getFromUserName()).fromUser(wxMessage.getToUserName());
		Matcher mat = pat.matcher(msg);

		root:
		if (mat.find()) {
			int id = Integer.parseInt(mat.group(1));
			try (Connection conn = SQLCore.ds.getConnection()) {
				switch (checkID(conn, id)) {
					case -1:
						out.content("无效ID。");
						break root;
					case -2:
						out.content("该ID已登记过。");
						break root;
				}
				if (checkDuplicated(conn, wxMessage.getFromUserName())) {
					out.content("你的微信已经登记过。");
					break root;
				}
				PreparedStatement ps = conn.prepareStatement("UPDATE auth SET wechat=? WHERE id=?");
				ps.setString(1, wxMessage.getFromUserName());
				ps.setInt(2, id);
				if (ps.executeUpdate() == 1) {
					out.content("登记成功。");
				} else {
					out.content("登记失败，请联系管理员。");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				out.content("系统异常，请联系管理员。");
			}
		} else {
			out.content("无效ID。");
		}
		return out.build();
	}

	private static int checkID(Connection conn, int id) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT wechat FROM auth WHERE id=?");
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			return rs.getString("wechat") != null ? -2 : 0;
		} else {
			return -1;
		}
	}

	private static boolean checkDuplicated(Connection conn, String wechat) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT wechat FROM auth WHERE wechat=?");
		ps.setString(1, wechat);
		ResultSet rs = ps.executeQuery();
		return rs.next();
	}

}