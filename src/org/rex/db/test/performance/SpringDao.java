package org.rex.db.test.performance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.rex.db.test.Student;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class SpringDao extends Dao{
	
	//--spring context
	private static ApplicationContext ctx  = new ClassPathXmlApplicationContext("applicationContext.xml");
	
	public static SpringDao getDao(){
		return (SpringDao)ctx.getBean("studentDao");
	}
	
	private JdbcTemplate template;

	public JdbcTemplate getTemplate() {
		return template;
	}

	public void setTemplate(JdbcTemplate template) {
		this.template = template;
	}
	
	//----table mapper
	static class StudentsRowMapper implements RowMapper{
		@Override
		public Object mapRow(ResultSet rs, int rownum) throws SQLException {
			Student student = new Student();
			student.setStudentId(rs.getInt("STUDENT_ID"));
			student.setName(rs.getString("NAME"));
			student.setSex(rs.getInt("SEX"));
			student.setBirthday(rs.getDate("BIRTHDAY"));
			student.setBirthTime(rs.getTime("BIRTH_TIME"));
			student.setEnrollmentTime(rs.getTimestamp("ENROLLMENT_TIME"));
			student.setMajor(rs.getInt("MAJOR"));
			student.setPhoto(rs.getBytes("PHOTO"));
			student.setRemark(rs.getString("REMARK"));
			student.setReadonly(rs.getInt("READONLY"));
			
			return student;
		}
	}

	//-----------------------implements
	public int insert() throws Exception {
		String sql = "INSERT INTO rexdb_test_student(STUDENT_ID, NAME, SEX, BIRTHDAY, BIRTH_TIME, ENROLLMENT_TIME, MAJOR, PHOTO, REMARK, READONLY) VALUES (?,?,?,?,?,?,?,?,?,?)";
		Student stu = newStudent();
		Object[] values = new Object[]{
			stu.getStudentId(),
			stu.getName(),
			stu.getSex(),
			new java.sql.Timestamp(stu.getBirthday().getTime()),
			new java.sql.Timestamp(stu.getBirthTime().getTime()),
			new java.sql.Timestamp(stu.getEnrollmentTime().getTime()),
			stu.getMajor(),
			stu.getPhoto(),
			stu.getRemark(),
			stu.getReadonly()
		};
		return template.update(sql, values);
	}

	@Override
	public int insertPs() throws Exception {
		return insert();
	}

	public List getList() throws Exception {
		return template.query("SELECT * FROM rexdb_test_student", new StudentsRowMapper());
	}

	public List getMapList() throws Exception {
		return template.queryForList("SELECT * FROM rexdb_test_student");
	}

	public int delete() throws Exception {
		return template.update("DELETE FROM rexdb_test_student");
	}

	public int[] batchInsert(int rows) throws Exception {
		String sql = "INSERT INTO rexdb_test_student(STUDENT_ID, NAME, SEX, BIRTHDAY, BIRTH_TIME, ENROLLMENT_TIME, MAJOR, PHOTO, REMARK, READONLY) VALUES (?,?,?,?,?,?,?,?,?,?)";
		List<Object[]> values = new ArrayList<Object[]>();
		for (int i = 0; i < rows; i++) {
			Student stu = newStudent();
			Object[] v = new Object[]{
				stu.getStudentId(),
				stu.getName(),
				stu.getSex(),
				new java.sql.Timestamp(stu.getBirthday().getTime()),
				new java.sql.Timestamp(stu.getBirthTime().getTime()),
				new java.sql.Timestamp(stu.getEnrollmentTime().getTime()),
				stu.getMajor(),
				stu.getPhoto(),
				stu.getRemark(),
				stu.getReadonly()
			};
			values.add(v);
		}
		
		return template.batchUpdate(sql, values);
	}
	
	@Override
	public int[] batchInsertPs(int rows) throws Exception {
		return batchInsert(rows);
	}

	@Override
	public String getName() throws Exception {
		return "spring";
	}

}