package test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rex.DB;
import org.rex.db.configuration.Configuration;
import org.rex.db.dialect.Dialect;
import org.rex.db.exception.DBException;

import com.alibaba.fastjson.JSON;

import test.performance.Dao;
import test.performance.HibernateDao;
import test.performance.JdbcDao;
import test.performance.MybatisDao;
import test.performance.RexdbDao;

public class RunTest {
	
	static DecimalFormat df =new DecimalFormat("#.00");  
	
	//--operation
	public static final int OPER_INSERT = 0;
	public static final int OPER_QUERY_LIST = 1;
	public static final int OPER_QUERY_MAPLIST = 2;
	public static final int OPER_BATCH = 3;
	
	//--daos
	Dao hibernateDao;
	Dao mybatisDao;
	Dao rexdbDao;
	Dao jdbcDao;
	
	//--framework enabled
	boolean hibernateEnabled, mybatisEnabled, rexdbEnabled, jdbcEnabled;
	
	public RunTest() throws Exception{
		hibernateDao = new HibernateDao();
		mybatisDao = new MybatisDao();
		rexdbDao = new RexdbDao();
		jdbcDao = new JdbcDao();
		
		rebuildTable();
		testFramework();
	}
	
	//--recreate table
	public void rebuildTable() throws Exception{
		System.out.println("================== creating table r_student ==================");
		
		Dialect dialect = DB.getDialect();
		if(dialect == null)
			throw new Exception("database not support, dialect required.");
		String name = dialect.getName();
		
		InputStream is = this.getClass().getResourceAsStream("/create/"+name.toLowerCase()+".sql");
		if(is == null)
			throw new Exception("file "+ "create/"+name.toLowerCase()+".sql" +" not exist.");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = in.readLine())!=null){
			if(!line.startsWith("--"))
				sb.append(line);
		}
		
		String[] sqls = sb.toString().split(";");
		
		DB.beginTransaction();
		try{
			for (int i = 0; i < sqls.length; i++) {
				DB.update(sqls[i]);
				System.out.println("--- execute: "+sqls[i]);
			}
			DB.commit();
		}catch(Exception e){
			DB.rollback();
			System.out.println("--- error: "+e.getMessage());
		}
	}
	
	//test frameworks
	public void testFramework() throws Exception{
		System.out.println("================== testing frameworks ==================");
		
		try{
			hibernateDao.delete();
			
			hibernateDao.insert();
			hibernateDao.batchInsert(1);
			hibernateDao.getList();
			hibernateDao.getMapList();
			hibernateDao.delete();
			
			hibernateEnabled = true;
		}catch(Exception e){
			System.out.println("-- hibernate error: " + e.getMessage());
			hibernateEnabled = false;
		}
		
		try{
			mybatisDao.insert();
			mybatisDao.batchInsert(1);
			mybatisDao.getList();
			mybatisDao.getMapList();
			mybatisDao.delete();
			
			mybatisEnabled = true;
		}catch(Exception e){
			System.out.println("-- mybatis error: " + e.getMessage());
			mybatisEnabled = false;
		}
		
		try{
			rexdbDao.insert();
			rexdbDao.batchInsert(1);
			rexdbDao.getList();
			rexdbDao.getMapList();
			rexdbDao.delete();
			
			rexdbEnabled = true;
		}catch(Exception e){
			System.out.println("-- rexdb error: " + e.getMessage());
			e.printStackTrace();
			rexdbEnabled = false;
		}
		
		try{
			jdbcDao.insert();
			jdbcDao.batchInsert(1);
			jdbcDao.getList();
			jdbcDao.getMapList();
			jdbcDao.delete();
			
			jdbcEnabled = true;
		}catch(Exception e){
			System.out.println("-- jdbc error: " + e.getMessage());
			jdbcEnabled = false;
		}
		
		System.out.println("--- hibernateEnabled: "+hibernateEnabled);
		System.out.println("--- mybatisEnabled: "+mybatisEnabled);
		System.out.println("--- rexdbEnabled: "+rexdbEnabled);
		System.out.println("--- jdbcEnabled: "+jdbcEnabled);
	}
	
	//remove all rows
	public void deleteRows() throws Exception{
		rexdbDao.delete();
	}
	
	//insert rows for test
	public void initRows(int rows) throws Exception{
		rexdbDao.batchInsert(rows);
	}
	
	public long oper(int operation, Dao dao, int rows) throws Exception{
		long start = System.currentTimeMillis();
		
		if(OPER_BATCH == operation){
			dao.batchInsert(rows);
		}else if(OPER_QUERY_LIST == operation){
			dao.getList();
		}else if(OPER_QUERY_MAPLIST == operation){
			dao.getMapList();
		}else if(OPER_INSERT == operation){
			for (int i = 0; i < rows; i++) {
				dao.insert();
			}
		}
		
		return System.currentTimeMillis() - start;
	}
	
	//test insert performance
	public double[] opers(String testName, int operation, int loop, int rows) throws Exception{
		System.out.println("-------------------------- testing "+testName+" ------------------------");
		System.out.println("|      |     rexdb     |     jdbc     |    hibernate    |  mybatis   |");
		System.out.println("| ---- | ------------- | ------------ | --------------- | ---------- |");
		
		List<Double> timeRs = new ArrayList<Double>(),
					timeJs = new ArrayList<Double>(),
					timeHs = new ArrayList<Double>(),
					timeMs = new ArrayList<Double>();
		
		for (int i = 0; i < loop; i++) {
			double h = 0, m = 0, r = 0, j = 0;
			double timeH, timeM, timeJ, timeR;
			
			if(rexdbEnabled) r = oper(operation, rexdbDao, rows);
			if(jdbcEnabled) j = oper(operation, jdbcDao, rows);
			if(hibernateEnabled) h = oper(operation, hibernateDao, rows);
			if(mybatisEnabled) m = oper(operation, mybatisDao, rows);
			
			timeR = rows/(r/1000);
			timeJ = rows/(j/1000);
			timeH = rows/(h/1000);
			timeM = rows/(m/1000);
			
			timeRs.add(timeR);
			timeJs.add(timeJ);
			timeHs.add(timeH);
			timeMs.add(timeM);
			
			System.out.println("|   " + (i + 1) + "  |     " + df.format(timeR) + "     |    " + df.format(timeJ) + "     |      " + df.format(timeH) + "      |   " + df.format(timeM) + "    |");
		}
		
		System.out.println("| AVG  |     " + avg(timeRs, loop) + "     |    " + avg(timeJs, loop) + "     |      " + avg(timeHs, loop) + "      |   " + avg(timeMs, loop) + "    |");
		return new double[]{new Double(avg(timeRs, loop)), new Double(avg(timeJs, loop)), new Double(avg(timeHs, loop)), new Double(avg(timeMs, loop))};
	}
	
	private static String avg(List<Double> times, int loop){
		Collections.sort(times);
		double count = 0;
		for (int i = 0; i < times.size(); i++) {
			count += times.get(i);
		}
		
		return df.format(count/loop);
	}
	
	//set rexdb dynamicClass setting
	public void  setRexdbDynamicClass(boolean dynamicClass) throws DBException{
		Configuration.getCurrentConfiguration().setDynamicClass(dynamicClass);
	}
	
	
	//----------START TESTING
	public static void main(String[] args) throws Exception {
		RunTest test = new RunTest();
		
		boolean fast = false;
		for (int i = 0; i < args.length; i++) {
			if("fast".equals(args[i]))
				fast = true;
		}
		
		Map<String, double[]> results = new LinkedHashMap<String, double[]>();
		
		//--------fast test
		test.deleteRows();
		int loop = fast ? 10 : 3;
			
		System.out.println("===================== running test ======================");
		
		//test insert
		results.put("insert", test.opers("insert", OPER_INSERT, loop, 200));
		test.deleteRows();
		
		//test batch insert
		results.put("batchInsert", test.opers("batchInsert", OPER_BATCH, loop, 50000));
		test.deleteRows();
		
		//test get list
		test.initRows(50000);
		results.put("getList", test.opers("getList", OPER_QUERY_LIST, loop, 50000));
		test.setRexdbDynamicClass(false);
		results.put("getList-disableDynamicClass", test.opers("getList-disableDynamic", OPER_QUERY_LIST, loop, 50000));
		test.setRexdbDynamicClass(true);
		results.put("getMapList", test.opers("getMapList", OPER_QUERY_MAPLIST, loop, 50000));
		
		test.deleteRows();
		
		//------print results
		printResult(results);
		printJson(results);
	}
	
	//print result
	public static void printResult(Map<String, double[]> result){
		System.out.println("================== printing result ==================");
		System.out.println("|   OPER/COSTS(ms)   |     rexdb     |     jdbc    |  hibernate |  mybatis |");
		System.out.println("| ------------------ | ------------- | ----------- | ---------- | -------- |");
		
		for (Iterator<Map.Entry<String, double[]>> iterator = result.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, double[]> entry = iterator.next();
			String key = entry.getKey();
			double[] values = entry.getValue();
			
			System.out.println("|   " + key + "   |     " + values[0] + "     |     " + values[1] + "     |   " + values[2] + "   |   " + values[3] + "   |");
		}
	}
	
	//print json
	public static void printJson(Map<String, double[]> result){
		System.out.println("================== printing json result ==================");
		
		Map datas = new LinkedHashMap();
		for (Iterator<Map.Entry<String, double[]>> iterator = result.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, double[]> entry = iterator.next();
			String key = entry.getKey();
			double[] values = entry.getValue();
			Map costs = new LinkedHashMap();
			costs.put("hibernate", values[0]);
			costs.put("mybatis", values[1]);
			costs.put("jdbc", values[2]);
			costs.put("rexdb", values[3]);
			
			datas.put(key, costs);
		}
		
		System.out.println(JSON.toJSONString(datas));
	}
}
