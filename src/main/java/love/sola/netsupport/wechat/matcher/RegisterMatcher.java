package love.sola.netsupport.wechat.matcher;

import love.sola.netsupport.pojo.User;
import love.sola.netsupport.sql.TableUser;
import me.chanjar.weixin.mp.api.WxMpMessageMatcher;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;

/**
 * ***********************************************
 * Created by Sola on 2015/11/26.
 * Don't modify this source without my agreement
 * ***********************************************
 */
public class RegisterMatcher implements WxMpMessageMatcher {

	@Override
	public boolean match(WxMpXmlMessage message) {
		String fromUser = message.getFromUserName();
		User u = TableUser.getUserByWechat(fromUser);
		return u == null;
	}

}