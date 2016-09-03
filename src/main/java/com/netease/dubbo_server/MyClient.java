package com.netease.dubbo_server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.netease.dubbo_client.service.AssignmentTask;
import com.netease.dubbo_server.service.SayHelloToClient;

public class MyClient {
	public static void main(String[] args) {

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "applicationConsumer.xml" });

		context.start();

		// 获取服务器那边的bean
		AssignmentTask assignmentTask = (AssignmentTask) context.getBean("assignmentTask");

		System.out.println(assignmentTask.newTask("lisi"));
	}

}
