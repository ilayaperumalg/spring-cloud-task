/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.task.listener;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.redis.config.RedisServiceAutoConfiguration;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
public class TaskEventTests {

	static CountDownLatch latch = new CountDownLatch(2);

	@Test
	public void testDefaultConfiguration() throws Exception {
		ConfigurableApplicationContext parentContext = new SpringApplicationBuilder()
				.sources(ListenerBinding.class, RedisServiceAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class).web(false).build("--spring.cloud.stream.defaultBinder=redis").run();
		EventSink sink = parentContext.getBeansOfType(EventSink.class).get("org.springframework.cloud.task.listener.TaskEventTests$EventSink");
		ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder().sources(new Object[] {TaskEventsConfiguration.class,
				TaskEventAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				RedisServiceAutoConfiguration.class}).build().run(new String[] {"--spring.cloud.task.closecontext.enable=false",
				"--spring.main.web-environment=false",
				"--spring.cloud.stream.defaultBinder=redis",
				"--spring.cloud.stream.bindings.task-events.destination=test",
				"--spring.cloud.stream.bindings.task-events.contentType=application/json"});
		assertNotNull(applicationContext.getBean("taskEventListener"));
		assertNotNull(applicationContext.getBean(TaskEventAutoConfiguration.TaskEventChannels.class));
		latch.await(1, TimeUnit.SECONDS);
	}

	@Configuration
	@EnableTask
	public static class TaskEventsConfiguration {
	}

	@EnableBinding(EventSink.class)
	@PropertySource("classpath:/org/springframework/cloud/task/listener/sink-channel.properties")
	public static class ListenerBinding {

		@StreamListener(EventSink.INPUT)
		public void receive(TaskExecution execution) {
			latch.countDown();
		}
	}

	public interface EventSink {

		public String INPUT = "test";

		@Input(EventSink.INPUT)
		SubscribableChannel input();
	}
}
