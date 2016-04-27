package love.sola.netsupport.enums;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static love.sola.netsupport.config.Lang.lang;

/**
 * @author Sola {@literal <dev@sola.love>}
 */
public class Access {

	public static final int GOD_MODE = -1;
	public static final int ROOT = 0;
	public static final int MANAGER = 1;
	public static final int CO_MANAGER = 2;
	public static final int LEADER = 3;
	public static final int CO_LEADER = 4;
	public static final int ELITE = 5;
	public static final int ELDER = 6;
	public static final int MEMBER = 7;
	public static final int PRE_MEMBER = 8;
	public static final int NO_LOGIN = 9;
	public static final int USER = 10;
	public static final int GUEST = 11;

	public static final Map<Integer, String> inverseMap = new HashMap<>();

	static{
		System.out.println("Loading Access...");
		for (Field field : Access.class.getDeclaredFields()) {
			if (field.getType().isAssignableFrom(Integer.TYPE)) {
				try {
					inverseMap.put((Integer) field.get(null), field.getName());
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static String getLocalized(int access) {
		if (inverseMap.containsKey(access)) {
			return lang("ACCESS_" + inverseMap.get(access));
		}
		return null;
	}

}
