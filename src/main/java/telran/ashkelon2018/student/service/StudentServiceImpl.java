package telran.ashkelon2018.student.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import telran.ashkelon2018.student.dao.StudentRepository;
import telran.ashkelon2018.student.domain.Student;
import telran.ashkelon2018.student.dto.ScoreDto;
import telran.ashkelon2018.student.dto.StudentDto;
import telran.ashkelon2018.student.dto.StudentEditDto;
import telran.ashkelon2018.student.dto.StudentForbiddenException;
import telran.ashkelon2018.student.dto.StudentNotFoundException;
import telran.ashkelon2018.student.dto.StudentResponseDto;
import telran.ashkelon2018.student.dto.StudentUnauthorized;

@Service
@ManagedResource  // this is for log. we need @ManagedAttribute below fieldes
public class StudentServiceImpl implements StudentService {
	
	
	private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);
	
	
	boolean logEnlab = true;
	
	@Value("${time:title}")
	private String title;

	
	@ManagedAttribute  // for logs
	public boolean isLogEnlab() {
		return logEnlab;
	}

	@ManagedAttribute   // for logs
	public void setLogEnlab(boolean logEnlab) {
		this.logEnlab = logEnlab;
	}

	@Autowired
	StudentRepository studentRepository;
	
	

	@Override
	public boolean addStudent(StudentDto studentDto) {
		if (studentRepository.existsById(studentDto.getId())) {
			return false;
		}
		Student student = new Student(studentDto.getId(), studentDto.getName(), studentDto.getPassword());
		studentRepository.save(student);
		return true;
	}

	@Override
	public StudentResponseDto deleteStudent(int id, String token) {
		Credentials credentials = decodeToken(token);
		if (credentials.id != id) {
			throw new StudentForbiddenException();
		}
		Student student = studentRepository.findById(id).get();
		studentRepository.deleteById(id);
		return convertToStudentResponseDto(student);
	}

	private Credentials decodeToken(String token) {
		try {
			int index = token.indexOf(" ");
			token = token.substring(index + 1);
			byte[] base64DecodeBytes = Base64.getDecoder().decode(token);
			token = new String(base64DecodeBytes);
			String[] auth = token.split(":");
			Credentials credentials = new Credentials(Integer.parseInt(auth[0]), auth[1]);
			Student student = studentRepository.findById(credentials.id).get();
			if (!credentials.password.equals(student.getPassword())) {
				throw new StudentUnauthorized();
			}
			return credentials;
		} catch (Exception e) {
			throw new StudentUnauthorized();
		}
	}

	private StudentResponseDto convertToStudentResponseDto(Student student) {

		return StudentResponseDto.builder().id(student.getId()).name(student.getName()).scores(student.getScores())
				.build();
	}

	@Override
	public StudentDto editStudent(int id, StudentEditDto studentEditDto, String token) {
		Credentials credentials = decodeToken(token);
		if (credentials.id != id) {
			throw new StudentForbiddenException();
		}
		Student student = studentRepository.findById(id).get();
		if (studentEditDto.getName() != null) {
			student.setName(studentEditDto.getName());
		}
		if (studentEditDto.getPassword() != null) {
			student.setPassword(studentEditDto.getPassword());
		}
		studentRepository.save(student);
		return convertToStudentDto(student);
	}

	private StudentDto convertToStudentDto(Student student) {
		return StudentDto.builder().id(student.getId()).name(student.getName()).password(student.getPassword()).build();
	}

	@Override
	public StudentResponseDto getStudent(int id) {
		return convertToStudentResponseDto(studentRepository
				.findById(id)
				.orElseThrow(StudentNotFoundException::new));
	}

	@Override
	public boolean addScore(int id, ScoreDto scoreDto) {
		LocalDateTime timeStamp = LocalDateTime.now();
		long t1 = System.currentTimeMillis();
		Student student = studentRepository
				.findById(id)
				.orElseThrow(StudentNotFoundException::new);
		boolean res = student.addScore(scoreDto.getExamName(),
				scoreDto.getScore());
		studentRepository.save(student);
		long t2 = System.currentTimeMillis();
		if(logEnlab) logger.info(title+": Time stamp+ "+timeStamp+" duation: "+ (t2-t1));
		return res;
	}

	@AllArgsConstructor
	private class Credentials {
		int id;
		String password;
	}

	@Override
	public List<StudentResponseDto> getStudentsByName(String name) {
	
		return studentRepository.findByNameRegex(name)
				.stream()
				.map(this::convertToStudentResponseDto)
				.collect(Collectors.toList());
	}
	
	@Override
	public List<StudentResponseDto> getStudentsByExam(String exam, int score) {
	
		return studentRepository.findByExam("scores."+exam, score)
				.stream()
				.map(this::convertToStudentResponseDto)
				.collect(Collectors.toList());
	}

}
