package com.baihui.studio;

/**
 * 定义数据库连接和关闭
 */

import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;


public class BaseDAO {

    public static Logger log = Logger.getLogger(BaseDAO.class);

    public static Connection conn = null;

    public static PreparedStatement pstmt = null;

    public static ResultSet rs = null;

    /**
     * 创建数据库连接--使用jdbc
     * @return
     * @throws java.sql.SQLException
     * @throws ClassNotFoundException
     */

	/*public static Connection getConnection() throws SQLException, ClassNotFoundException {
        log.info("BaseDAO/getConnection() begin.");
		String jdbc_driver="com.mysql.jdbc.Driver";

		 //测试环境
		String jdbc_url="jdbc:mysql://192.168.111.141:3306/haoshili?characterEncoding=utf-8";
		String jdbc_username="bhdc001admin";
		String jdbc_password="baihuizhadmin";


		// 正式环境
//		String jdbc_url="jdbc:mysql://192.168.101.100:3306/haoshili?characterEncoding=utf-8";
//		String jdbc_username="root";
//		String jdbc_password="mysql#kabahui";
		log.info("jdbc_url:[" + jdbc_url + "]");
		log.info("jdbc_username:[" + jdbc_username + "]");
		log.info("jdbc_password:[" + jdbc_password + "]");
		Class.forName(jdbc_driver);

		log.info("BaseDAO/getConnection() end");
		return DriverManager.getConnection(jdbc_url, jdbc_username, jdbc_password);
	}*/

    /**
     * 创建数据库连接--使用jndi
     *
     * @return
     * @throws java.sql.SQLException
     * @throws ClassNotFoundException
     */
    public static Connection getConnection() {
        Context ctx = null;
        DataSource ds = null;
        Connection conn = null;
        try {
            ctx = new InitialContext();
            String jndi = PropertyManager.getProperty("JNDI");
            ds = (DataSource) ctx.lookup(jndi);
            conn = ds.getConnection();
        } catch (NamingException e) {
            log.error("DB/getConnection NamingException:", e);
        } catch (SQLException e1) {
            log.error("DB/getConnection SQLException:", e1);
        }
        return conn;
    }

    /**
     * 通过sql和参数执行查询SQL
     *
     * @param sql
     * @param params
     * @return 返回查询SQL的ResultSet对象
     * @throws java.sql.SQLException
     * @throws Exception
     * @Title: sqlQueryList
     * @author jason
     * @date 2013-11-29 下午02:34:33
     */
    public ResultSet sqlQuery(final String sql, Object... params) throws SQLException, Exception {
        log.info("BaseDAO/sqlQuery(String sql, Object... params) begin.");
        log.info("sql:[" + sql + "]");
        try {
            conn = BaseDAO.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params != null) {
                fillStatement(pstmt, params);
            }
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            log.error("sql:" + sql);
            log.error("error", e);
            log.info("BaseDAO/sqlQuery(String sql, Object... params) end");
            throw e;
        } catch (Exception e) {
            log.error("sql:" + sql);
            log.error("error", e);
            log.info("BaseDAO/sqlQuery(String sql, Object... params) end");
            throw e;
        }
        log.info("BaseDAO/sqlQuery(String sql, Object... params) end");
        return rs;
    }

    /**
     * 执行单条sql(增删改)
     *
     * @param sql
     * @param params
     * @return sql执行影响记录数
     * @throws java.sql.SQLException
     * @throws Exception             int
     * @Title: sqlExecute
     * @author jason
     * @date 2013-11-30 上午10:04:03
     */
    public int sqlExecute(final String sql, Object... params) throws SQLException, Exception {
        log.info("BaseDAO/sqlExecute(String sql, Object... params) begin.");
        log.info("sql:[" + sql + "]");
        try {
            conn = BaseDAO.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params != null) {
                fillStatement(pstmt, params);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("sql:" + sql);
            log.error("error", e);
            throw e;
        } catch (Exception e) {
            log.error("sql:" + sql);
            log.error("error", e);
            throw e;
        } finally {
            closeAll();
            log.info("BaseDAO/sqlExecute(String sql, Object... params) end.");
        }
    }

    /**
     * 批量执行sql(增删改)
     *
     * @param sql
     * @param list
     * @return 批量执行sql(增删改)各自的影响记录数
     * @throws Exception int[]
     * @Title: sqlBatchExcute
     * @author jasonk
     * @date 2013-11-30 上午10:04:50
     */
    public int[] sqlBatchExecute(final String sql, List<Object[]> objectArrList) throws Exception {
        log.info("BaseDAO/sqlBatchExecute(final String sql, List<Object[]> objectArrList) begin.");
        log.info("sql:[" + sql + "]");
        try {
            conn = BaseDAO.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (objectArrList != null && !objectArrList.isEmpty()) {
                for (int i = 0; i < objectArrList.size(); i++) {
                    Object[] everyParams = objectArrList.get(i);
                    fillStatement(pstmt, everyParams);
                    pstmt.addBatch();
                    //批量操作数据库
                    if (i != 0 && i % Constants.DB_BATCH_DO_NUM == 0) {
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }
            }
            conn.setAutoCommit(true);
            return pstmt.executeBatch();
        } catch (SQLException e) {
            log.error("sql:" + sql);
            log.error("error", e);
            throw e;
        } catch (Exception e) {
            log.error("sql:" + sql);
            log.error("error", e);
            throw e;
        } finally {
            closeAll();
            log.info("BaseDAO/sqlBatchExecute(final String sql, List<Object[]> objectArrList) end.");
        }
    }

    /**
     * 获取数据库的当前时间
     *
     * @return
     * @throws Exception
     */
    public static Timestamp getSystemTimestamp() throws Exception {
        log.info("BaseDAO/getSystemTimestamp() begin.");
        String sql = "select sysdate()";
        ResultSet rs = null;
        try {
            rs = new BaseDAO().sqlQuery(sql);
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        } finally {
            closeAll();
            log.info("BaseDAO/getSystemTimestamp() end.");
        }
        return null;
    }

    /**
     * 拼接增删改查的参数
     *
     * @param ps
     * @param params
     * @throws java.sql.SQLException
     */
    protected void fillStatement(PreparedStatement ps, Object[] params)
            throws SQLException {
        if (params == null) {

            log.info("BaseDAO/fillStatement(PreparedStatement ps, Object[] params) end.");
            return;
        }
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null || "".equals(String.valueOf(params[i]).trim())
                    || "null".equals(String.valueOf(params[i]).trim())) {
                params[i] = "";
            }
            ps.setObject(i + 1, params[i]);
        }
    }

    /**
     * 关闭全部(ResultSet,Statement,Connection)
     *
     * @throws java.sql.SQLException
     * @Title: closeAll void
     * @author jason
     * @date 2013-11-30 上午10:05:40
     */
    public static void closeAll() throws SQLException {
        closeResultSet(rs);
        closeStatement(pstmt);
        closeConnection(conn);
    }

    /**
     * 关闭数据库链接
     *
     * @throws java.sql.SQLException
     */
    public static void closeConnection(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    /**
     * 关闭执行语句
     *
     * @param ps
     * @throws java.sql.SQLException
     */
    public static void closeStatement(PreparedStatement ps) throws SQLException {
        if (ps != null) {
            ps.close();
        }
    }

    /**
     * 关闭结果集
     *
     * @throws java.sql.SQLException
     */
    public static void closeResultSet(ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
    }

    public static void main(String[] args) {

    }


    /**
     * @param insertParamsList
     * @return int[]
     * @throws Exception
     * @Title: insert
     * @author jason
     * @date 2013-12-5 上午11:52:12
     */
    public int[] insert(List<Object[]> insertParamsList) throws Exception {
        return null;
    }


    /**
     * @param updateParamsList
     * @return int[]
     * @Title: update
     * @author jason
     * @date 2013-12-5 上午11:52:31
     */
    public int[] update(List<Object[]> updateParamsList) throws Exception {
        return null;
    }

}
