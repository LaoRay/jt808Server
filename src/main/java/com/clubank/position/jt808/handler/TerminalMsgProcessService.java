package com.clubank.position.jt808.handler;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.clubank.position.jt808.util.HexStringUtils;
import com.clubank.position.jt808.util.MsgEncoder;
import com.clubank.position.jt808.util.PropertyUtil;
import com.clubank.position.jt808.vo.PackageData;
import com.clubank.position.jt808.vo.PackageData.MsgHeader;
import com.clubank.position.jt808.vo.Session;
import com.clubank.position.jt808.vo.req.LocationInfoUploadMsg;
import com.clubank.position.jt808.vo.req.TerminalAuthenticationMsg;
import com.clubank.position.jt808.vo.req.TerminalRegisterMsg;
import com.clubank.position.jt808.vo.resp.ServerCommonRespMsgBody;
import com.clubank.position.jt808.vo.resp.TerminalRegisterMsgRespBody;

import lombok.extern.slf4j.Slf4j;

/**
 * 消息处理
 */
@Slf4j
public class TerminalMsgProcessService extends BaseMsgProcessService {

	private MsgEncoder msgEncoder;
	private SessionManager sessionManager;

	public TerminalMsgProcessService() {
		this.msgEncoder = new MsgEncoder();
		this.sessionManager = SessionManager.getInstance();
	}

	public void processRegisterMsg(TerminalRegisterMsg msg) throws Exception {
		log.debug("终端注册:{}", JSON.toJSONString(msg, true));

		final String sessionId = Session.buildId(msg.getChannel());
		Session session = sessionManager.findBySessionId(sessionId);
		if (session == null) {
			session = Session.buildSession(msg.getChannel(), msg.getMsgHeader().getTerminalNo());
		}
		session.setAuthenticated(true);
		session.setTerminalPhone(msg.getMsgHeader().getTerminalNo());
		sessionManager.put(session.getId(), session);

		TerminalRegisterMsgRespBody respMsgBody = new TerminalRegisterMsgRespBody();
		respMsgBody.setReplyCode(TerminalRegisterMsgRespBody.success);
		respMsgBody.setReplyFlowId(msg.getMsgHeader().getFlowId());
		// 鉴权码
		respMsgBody.setReplyToken(PropertyUtil.getProperty("auth_code"));
		int flowId = super.getFlowId(msg.getChannel());
		byte[] bs = this.msgEncoder.encode4TerminalRegisterResp(msg, respMsgBody, flowId);
		log.info("注册响应,{}", HexStringUtils.bytesToHexFun1(bs));
		super.send2Client(msg.getChannel(), bs);
	}

	public void processAuthMsg(TerminalAuthenticationMsg msg) throws Exception {
		// TODO 暂时每次鉴权都成功
		if (!StringUtils.equals(msg.getAuthCode(), PropertyUtil.getProperty("auth_code"))) {
			// 鉴权失败
			log.warn("终端鉴权失败，鉴权码:{}", msg.getAuthCode());
		}
		log.debug("终端鉴权:{}", JSON.toJSONString(msg, true));

		final String sessionId = Session.buildId(msg.getChannel());
		Session session = sessionManager.findBySessionId(sessionId);
		if (session == null) {
			session = Session.buildSession(msg.getChannel(), msg.getMsgHeader().getTerminalNo());
		}
		session.setAuthenticated(true);
		session.setTerminalPhone(msg.getMsgHeader().getTerminalNo());
		sessionManager.put(session.getId(), session);

		ServerCommonRespMsgBody respMsgBody = new ServerCommonRespMsgBody();
		respMsgBody.setReplyCode(ServerCommonRespMsgBody.success);
		respMsgBody.setReplyFlowId(msg.getMsgHeader().getFlowId());
		respMsgBody.setReplyId(msg.getMsgHeader().getMsgId());
		int flowId = super.getFlowId(msg.getChannel());
		byte[] bs = this.msgEncoder.encode4ServerCommonRespMsg(msg, respMsgBody, flowId);
		log.info("鉴权响应", HexStringUtils.bytesToHexFun1(bs));
		super.send2Client(msg.getChannel(), bs);
	}

	public void processTerminalHeartBeatMsg(PackageData req) throws Exception {
		log.debug("心跳信息:{}", JSON.toJSONString(req, true));
		final MsgHeader reqHeader = req.getMsgHeader();
		ServerCommonRespMsgBody respMsgBody = new ServerCommonRespMsgBody(reqHeader.getFlowId(), reqHeader.getMsgId(),
				ServerCommonRespMsgBody.success);
		int flowId = super.getFlowId(req.getChannel());
		byte[] bs = this.msgEncoder.encode4ServerCommonRespMsg(req, respMsgBody, flowId);
		log.info("心跳响应:{}", HexStringUtils.bytesToHexFun1(bs));
		super.send2Client(req.getChannel(), bs);
	}

	public void processTerminalLogoutMsg(PackageData req) throws Exception {
		log.info("终端注销:{}", JSON.toJSONString(req, true));
		final MsgHeader reqHeader = req.getMsgHeader();
		ServerCommonRespMsgBody respMsgBody = new ServerCommonRespMsgBody(reqHeader.getFlowId(), reqHeader.getMsgId(),
				ServerCommonRespMsgBody.success);
		int flowId = super.getFlowId(req.getChannel());
		byte[] bs = this.msgEncoder.encode4ServerCommonRespMsg(req, respMsgBody, flowId);
		log.info("注销响应:{}", HexStringUtils.bytesToHexFun1(bs));
		super.send2Client(req.getChannel(), bs);
	}

	public void processLocationInfoUploadMsg(LocationInfoUploadMsg req) throws Exception {
		log.debug("位置信息:{}",
				JSON.toJSONStringWithDateFormat(req, "yyyy-MM-dd HH:mm:ss", SerializerFeature.PrettyFormat));
		final MsgHeader reqHeader = req.getMsgHeader();
		ServerCommonRespMsgBody respMsgBody = new ServerCommonRespMsgBody(reqHeader.getFlowId(), reqHeader.getMsgId(),
				ServerCommonRespMsgBody.success);
		int flowId = super.getFlowId(req.getChannel());
		byte[] bs = this.msgEncoder.encode4ServerCommonRespMsg(req, respMsgBody, flowId);
		log.info("位置响应:{}", HexStringUtils.bytesToHexFun1(bs));
		super.send2Client(req.getChannel(), bs);
	}
}
