package test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rex.DB;
import org.rex.db.dialect.Dialect;

import test.performance.Dao;
import test.performance.HibernateDao;
import test.performance.JdbcDao;
import test.performance.MybatisDao;
import test.performance.RexdbDao;

public class RunTest {
	
	//--operation
	public static final int OPER_INSERT = 0;
	public static final int OPER_QUERY_LIST = 1;
	public static final int OPER_QUERY_MAPLIST = 2;
	public static final int OPER_BATCH = 2;
	
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
			throw e;
		}
	}
	
	//test frameworks
	public void testFramework() throws Exception{
		System.out.println("================== testing frameworks ==================");
		
		try{
			hibernateDao.insert();
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
			rexdbDao.getList();
			rexdbDao.getMapList();
			rexdbDao.delete();
			
			rexdbEnabled = true;
		}catch(Exception e){
			System.out.println("-- rexdb error: " + e.getMessage());
			rexdbEnabled = false;
		}
		
		try{
			jdbcDao.insert();
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
		System.out.println("================== deleting all rows ==================");
		
		rexdbDao.delete();
	}
	
	//insert rows for test
	public void initRows(int rows) throws Exception{
		System.out.println("================== batch insert " + rows + " rows ==================");
		
		rexdbDao.batchInsert(rows);
	}
	
	public long oper(int operation, Dao dao, int rows) throws Exception{
		long start = System.currentTimeMillis();
		
		if(OPER_BATCH == operation){
			dao.batchInsert(rows);
		}else{
			for (int i = 0; i < rows; i++) {
				if(OPER_INSERT == operation){
					dao.insert();
				}else if(OPER_QUERY_LIST == operation){
					dao.getList();
				}else if(OPER_QUERY_MAPLIST == operation){
					dao.getMapList();
				}
			}
		}
		
		return System.currentTimeMillis() - start;
	}
	
	//test insert performance
	public long[] opers(String testName, int operation, int loop, int rows) throws Exception{
		System.out.println("--------------- testing "+testName+" ---------------");
		System.out.println("|      |   hibernate   |   mybatis   |    jdbc    |   rexdb  |");
		System.out.println("| ---- | ------------- | ----------- | ---------- | -------- |");
		
		long sumH = 0, sumM = 0, sumR = 0, sumJ = 0;
		for (int i = 0; i < loop; i++) {
			long h = 0, m = 0, r = 0, j = 0;
			
			h = oper(operation, hibernateDao, rows);
			m = oper(operation, mybatisDao, rows);
			j = oper(operation, jdbcDao, rows);
			r = oper(operation, rexdbDao, rows);
			
			sumH += h;
			sumM += m;
			sumJ += j;
			sumR += r;
			
			System.out.println("|   " + (i + 1) + "   |     " +h + "     |     " + m + "     |   " + j + "   |   " + r + "   |");
		}
		
		System.out.println("|  AVG   |     " + sumH/loop + "     |     " + sumM/loop + "     |   " + sumJ/loop + "   |   " + sumR/loop + "   |");
		return new long[]{sumH/loop, sumM/loop, sumJ/loop, sumR/loop};
	}
	
	
	//----------START TESTING
	public static void main(String[] args) throws Exception {
		RunTest test = new RunTest();
		
		System.out.println("================== running all test ==================");
		
		Map<String, long[]> results = new LinkedHashMap<String, long[]>();
		
		//test insert
		results.put("insert-100", test.opers("insert-100", OPER_INSERT, 100, 1));	//100
		results.put("insert-200", test.opers("insert-200", OPER_INSERT, 100, 2));	//200
		results.put("insert-500", test.opers("insert-500", OPER_INSERT, 100, 5));	//500
		test.deleteRows();
		
		//test batch insert
		results.put("batchInsert-10k", test.opers("batchInsert-10k", OPER_BATCH, 100, 100));	//10000
		results.put("batchInsert-50k", test.opers("batchInsert-50k", OPER_BATCH, 100, 500));	//10000
		results.put("batchInsert-100k", test.opers("batchInsert-100k", OPER_BATCH, 100, 1000));	//10000
		test.deleteRows();
		
		//test get list
		test.initRows(100);
		results.put("getList-10k", test.opers("getList-10k", OPER_QUERY_LIST, 100, 1));	//10000
		results.put("getMapList-10k", test.opers("getMapList-10k", OPER_QUERY_MAPLIST, 100, 1));	//10000
	
		test.deleteRows();
		test.initRows(500);
		results.put("getList-50k", test.opers("getList-50k", OPER_QUERY_LIST, 100, 1));	//10000
		results.put("getMapList-50k", test.opers("getMapList-50k", OPER_QUERY_MAPLIST, 100, 1));	//10000
		
		test.deleteRows();
		test.initRows(1000);
		results.put("getList-100k", test.opers("getList-100k", OPER_QUERY_LIST, 100, 1));	//10000
		results.put("getMapList-100k", test.opers("getMapList-100k", OPER_QUERY_MAPLIST, 100, 1));	//10000
		test.deleteRows();
		
		//------print results
		printResult(results);
	}
	
	//print result
	public static void printResult(Map<String, long[]> result){
		System.out.println("================== printing result ==================");
		System.out.println("|   OPER/COSTS(ms)   |   hibernate   |   mybatis   |    jdbc    |   rexdb  |");
		System.out.println("| ------------------ | ------------- | ----------- | ---------- | -------- |");
		
		for (Iterator<Map.Entry<String, long[]>> iterator = result.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, long[]> entry = iterator.next();
			String key = entry.getKey();
			long[] values = entry.getValue();
			
			System.out.println("|   " + key + "   |     " + values[0] + "     |     " + values[1] + "     |   " + values[2] + "   |   " + values[3] + "   |");
		}
	}
}
