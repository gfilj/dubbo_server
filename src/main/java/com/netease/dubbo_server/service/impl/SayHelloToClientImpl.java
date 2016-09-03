package com.netease.dubbo_server.service.impl;

import com.netease.dubbo_server.service.SayHelloToClient;

public class SayHelloToClientImpl implements SayHelloToClient{

	@Override
	public String sayHello(String hello) {
		System.out.println("我接收到了：" + hello);  
        return hello + "你也好啊！！！" ;  
	}

}
