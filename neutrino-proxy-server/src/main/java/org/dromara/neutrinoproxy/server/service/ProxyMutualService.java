/**
 * Copyright (c) 2022 aoshiguchen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dromara.neutrinoproxy.server.service;

import org.dromara.neutrinoproxy.server.constant.OnlineStatusEnum;
import org.dromara.neutrinoproxy.server.dal.LicenseMapper;
import org.dromara.neutrinoproxy.server.dal.PortMappingMapper;
import org.dromara.neutrinoproxy.server.proxy.domain.CmdChannelAttachInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.util.Date;

/**
 * 代理交互服务
 * @author: aoshiguchen
 * @date: 2022/9/3
 */
@Slf4j
@Component
public class ProxyMutualService {
	@Db
	private PortMappingMapper portMappingMapper;
	@Db
	private LicenseMapper licenseMapper;

	/**
	 * 客户端上线
	 * @param attachInfo
	 * @param serverPort
	 */
	public void online(CmdChannelAttachInfo attachInfo, Integer serverPort) {
		Date now = new Date();
		portMappingMapper.updateOnlineStatus(attachInfo.getLicenseId(), serverPort, OnlineStatusEnum.ONLINE.getStatus(), now);
		licenseMapper.updateOnlineStatus(attachInfo.getLicenseId(), OnlineStatusEnum.ONLINE.getStatus(), now);
		log.info("bind server port licenseId:{},ip:{},serverPort:{}", attachInfo.getLicenseId(), attachInfo.getIp(),  serverPort);
	}

	/**
	 * 客户端下线
	 * @param attachInfo
	 */
	public void offline(CmdChannelAttachInfo attachInfo) {
		Date now = new Date();
		portMappingMapper.updateOnlineStatus(attachInfo.getLicenseId(), OnlineStatusEnum.OFFLINE.getStatus(), now);
		licenseMapper.updateOnlineStatus(attachInfo.getLicenseId(), OnlineStatusEnum.OFFLINE.getStatus(), now);
		log.info("client offline licenseId:{},ip:{}", attachInfo.getLicenseId(), attachInfo.getIp());
	}

}
