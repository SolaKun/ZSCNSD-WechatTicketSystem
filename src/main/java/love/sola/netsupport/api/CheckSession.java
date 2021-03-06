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

package love.sola.netsupport.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import love.sola.netsupport.enums.Access;
import love.sola.netsupport.enums.Attribute;
import love.sola.netsupport.session.WxSession;

/**
 * @author Sola {@literal <dev@sola.love>}
 */
public class CheckSession extends API {

    public CheckSession() {
        url = "/checksession";
        access = Access.GUEST;
        authorize = null;
    }

    @Override
    protected Object process(HttpServletRequest req, WxSession session) throws Exception {
        String more = req.getParameter("more");
        Map<String, Object> result = new HashMap<>();
        result.put(Attribute.AUTHORIZED, session.getAttribute(Attribute.AUTHORIZED));
        if (more != null) {
            switch (more) {
                case "1":
                    result.put(Attribute.USER, session.getAttribute(Attribute.USER));
                    result.put(Attribute.OPERATOR, session.getAttribute(Attribute.OPERATOR));
                    break;
            }
        }
        return result;
    }

}
