package love.sola.netsupport.sql;

import love.sola.netsupport.pojo.Operator;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * ***********************************************
 * Created by Sola on 2015/12/12.
 * Don't modify this source without my agreement
 * ***********************************************
 */
public class TableOperator extends SQLCore {

	public static boolean has(String wechat) {
		try (Session s = SQLCore.sf.openSession()) {
			return (long) s.createCriteria(Operator.class)
					.add(Restrictions.eq(Operator.PROPERTY_WECHAT, wechat))
					.setProjection(Projections.rowCount())
					.uniqueResult() > 0;
		}
	}


}
