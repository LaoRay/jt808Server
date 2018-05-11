package com.clubank.position.jt808.handler;

import com.alibaba.fastjson.JSON;
import com.clubank.position.jt808.common.JT808Consts;
import com.clubank.position.jt808.util.HexStringUtils;
import com.clubank.position.jt808.util.HttpUtil;
import com.clubank.position.jt808.util.JT808ProtocolUtils;
import com.clubank.position.jt808.util.MsgDecoder;
import com.clubank.position.jt808.util.PropertyUtil;
import com.clubank.position.jt808.vo.GpsLocations;
import com.clubank.position.jt808.vo.PackageData;
import com.clubank.position.jt808.vo.PackageData.MsgHeader;
import com.clubank.position.jt808.vo.Session;
import com.clubank.position.jt808.vo.req.LocationInfoUploadMsg;
import com.clubank.position.jt808.vo.req.TerminalAuthenticationMsg;
import com.clubank.position.jt808.vo.req.TerminalRegisterMsg;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息接收
 */
@Slf4j
public class TCPServerHandler extends ChannelInboundHandlerAdapter { // (1)

	private final SessionManager sessionManager;
	private final MsgDecoder decoder;
	private TerminalMsgProcessService msgProcessService;
	private final JT808ProtocolUtils jt808ProtocolUtils;

	public TCPServerHandler() {
		this.sessionManager = SessionManager.getInstance();
		this.decoder = new MsgDecoder();
		this.msgProcessService = new TerminalMsgProcessService();
		this.jt808ProtocolUtils = new JT808ProtocolUtils();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException { // (2)
		try {
			ByteBuf buf = (ByteBuf) msg;
			if (buf.readableBytes() <= 0) {
				return;
			}

			byte[] bs = new byte[buf.readableBytes()];
			buf.readBytes(bs);

			// 字节数据转换为jt808消息结构的实体类
			// 记录日志
			log.info("接收消息：7E{}7E", HexStringUtils.bytesToHexFun1(bs));
			// 接收消息时转义
			bs = this.jt808ProtocolUtils.doEscape4Receive(bs, 0, bs.length - 1);
			// 解析消息
			PackageData pkg = this.decoder.bytes2PackageData(bs);
			// 引用channel,以便回送数据给硬件
			pkg.setChannel(ctx.channel());
			this.processPackageData(pkg);
		} catch (Exception e) {
			log.error("消息转义错误", e);
		} finally {
			release(msg);
		}
	}

	/**
	 * 
	 * 处理业务逻辑
	 * 
	 * @param packageData
	 * 
	 */
	private void processPackageData(PackageData packageData) {
		final MsgHeader header = packageData.getMsgHeader();

		// 终端心跳-消息体为空 ==> 平台通用应答
		if (JT808Consts.msg_id_terminal_heart_beat == header.getMsgId()) {
			log.info(">>>>>[终端心跳],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			try {
				this.msgProcessService.processTerminalHeartBeatMsg(packageData);
				log.info("<<<<<[终端心跳],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			} catch (Exception e) {
				log.error("<<<<<[终端心跳]处理错误,terminalNo={},flowid={},err={}", header.getTerminalNo(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 终端鉴权 ==> 平台通用应答
		else if (JT808Consts.msg_id_terminal_authentication == header.getMsgId()) {
			log.info(">>>>>[终端鉴权],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			try {
				TerminalAuthenticationMsg authenticationMsg = new TerminalAuthenticationMsg(packageData);
				this.msgProcessService.processAuthMsg(authenticationMsg);
				log.info("<<<<<[终端鉴权],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			} catch (Exception e) {
				log.error("<<<<<[终端鉴权]处理错误,terminalNo={},flowid={},err={}", header.getTerminalNo(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 终端注册 ==> 终端注册应答
		else if (JT808Consts.msg_id_terminal_register == header.getMsgId()) {
			log.info(">>>>>[终端注册],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			try {
				TerminalRegisterMsg msg = this.decoder.toTerminalRegisterMsg(packageData);
				this.msgProcessService.processRegisterMsg(msg);
				log.info("<<<<<[终端注册],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			} catch (Exception e) {
				log.error("<<<<<[终端注册]处理错误,terminalNo={},flowid={},err={}", header.getTerminalNo(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 终端注销(终端注销数据消息体为空) ==> 平台通用应答
		else if (JT808Consts.msg_id_terminal_log_out == header.getMsgId()) {
			log.info(">>>>>[终端注销],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			try {
				this.msgProcessService.processTerminalLogoutMsg(packageData);
				log.info("<<<<<[终端注销],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			} catch (Exception e) {
				log.error("<<<<<[终端注销]处理错误,terminalNo={},flowid={},err={}", header.getTerminalNo(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 位置信息汇报 ==> 平台通用应答
		else if (JT808Consts.msg_id_terminal_location_info_report == header.getMsgId()) {
			log.info(">>>>>[位置信息],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			try {
				LocationInfoUploadMsg locationInfoUploadMsg = this.decoder.toLocationInfoUploadMsg(packageData);
				System.out.println(locationInfoUploadMsg);
				this.msgProcessService.processLocationInfoUploadMsg(locationInfoUploadMsg);

				GpsLocations gpsLocations = assembGpsLocations(header, locationInfoUploadMsg);

				// 将数据传输到业务服务器
				HttpUtil.post(PropertyUtil.getProperty("url"),
						JSON.toJSONStringWithDateFormat(gpsLocations, "yyyy-MM-dd HH:mm:ss"));

				log.info("<<<<<[位置信息],terminalNo={},flowid={}", header.getTerminalNo(), header.getFlowId());
			} catch (Exception e) {
				log.error("<<<<<[位置信息]处理错误,terminalNo={},flowid={},err={}", header.getTerminalNo(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 其他情况
		else {
			log.warn(">>>>>>[未知消息类型],terminalNo={},msgId={},package={}", header.getTerminalNo(), header.getMsgId(),
					packageData);
		}
	}

	private GpsLocations assembGpsLocations(MsgHeader header, LocationInfoUploadMsg locationMsg) {
		GpsLocations gps = new GpsLocations();
		gps.setTerminalNo(header.getTerminalNo());
		gps.setLatitude(locationMsg.getLatitude());
		gps.setLongitude(locationMsg.getLongitude());
		gps.setElevation(locationMsg.getElevation());
		gps.setSpeed(locationMsg.getSpeed());
		gps.setDirection(locationMsg.getDirection());
		gps.setTime(locationMsg.getTime());
		return gps;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		log.error("发生异常:{},异常终端:{}", cause, Session.buildSession(ctx.channel()));
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Session session = Session.buildSession(ctx.channel());
		sessionManager.put(session.getId(), session);
		log.debug("终端连接:{}", session);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final String sessionId = ctx.channel().id().asLongText();
		Session session = sessionManager.findBySessionId(sessionId);
		this.sessionManager.removeBySessionId(sessionId);
		log.debug("终端断开连接:{}", session);
		ctx.channel().close();
		// ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				Session session = this.sessionManager.removeBySessionId(Session.buildId(ctx.channel()));
				log.error("服务器主动断开连接:{}", session);
				ctx.close();
			}
		}
	}

	private void release(Object msg) {
		try {
			ReferenceCountUtil.release(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}