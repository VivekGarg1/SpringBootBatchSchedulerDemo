package com.home.app.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.home.app.model.Person;
import com.home.app.model.PersonRowMapper;
import com.home.app.processor.PersonItemProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;

	@Bean
	public JdbcCursorItemReader<Person> reader() {
		JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReader<>();
		itemReader.setDataSource(dataSource);
		itemReader.setSql("select person_id,first_name,last_name,email,age from person");
		itemReader.setRowMapper(new PersonRowMapper());
		return itemReader;
	}

	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}

	@Bean
	public FlatFileItemWriter<Person> writer() {
		FlatFileItemWriter<Person> itemWriter = new FlatFileItemWriter<>();
		itemWriter.setResource(new ClassPathResource("persons.csv"));

		DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter(",");

		BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] { "personId", "firstName", "lastName", "email", "age" });
		lineAggregator.setFieldExtractor(fieldExtractor);
		itemWriter.setLineAggregator(lineAggregator);
		return itemWriter;
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<Person, Person>chunk(100).reader(reader()).processor(processor())
				.writer(writer()).build();
	}

	@Bean
	public Job exportPersonJob() {
		return jobBuilderFactory.get("exportPersonJob").incrementer(new RunIdIncrementer()).flow(step1()).end().build();
	}

}