package com.ssafy.pjt.controller;

import com.ssafy.pjt.dto.Entrant;
import com.ssafy.pjt.dto.request.UpdateEntrantDto;
import com.ssafy.pjt.dto.request.insertEntrantDto;
import com.ssafy.pjt.repository.EntrantRepository;

import io.swagger.annotations.ApiOperation;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

@RestController
@CrossOrigin
@RequestMapping("/api/entrant")
public class EntrantController {

	@Autowired // This means to get the bean called userRepository
	// Which is auto-generated by Spring, we will use it to handle the data
	private EntrantRepository entrantRepository;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private AmqpAdmin admin;

	@ApiOperation(value = "방  리스트 조회")
	@GetMapping(path = "/findAll")
	public ResponseEntity<Object> findAll() {
		return new ResponseEntity<>(entrantRepository.findAll(), HttpStatus.OK);
	}

	@ApiOperation(value = "방  참가한 사람 조회")
	@GetMapping(path = "/findCount")
	public ResponseEntity<Object> findMember(@RequestParam int rid) {
		return new ResponseEntity<>(entrantRepository.findByRid(rid).size(), HttpStatus.OK);
	}

	@ApiOperation(value = "참여자가 방에 참여했는지 여부 확인")
	@GetMapping(path = "/findUidAndRid")
	public ResponseEntity<Object> findUidAndRid(@RequestParam int uid, @RequestParam int rid) {
		try {
			Entrant entrant = entrantRepository.findByUidAndRid(uid, rid);
			if (entrant != null) {
				return new ResponseEntity<>(entrant, HttpStatus.OK);
			} else {
				return new ResponseEntity<>("참가자 명단에 없습니다.", HttpStatus.NO_CONTENT);
			}
		} catch (Exception e) {
			return new ResponseEntity<>("fail", HttpStatus.BAD_REQUEST);
		}
	}

	@ApiOperation(value = "참가자 생성")
	@Transactional
	@PostMapping(path = "/insert")
	public ResponseEntity<Object> insertByUid(@RequestBody insertEntrantDto insertRoom) {
		Entrant entran = entrantRepository.findByUidAndRid(insertRoom.getUid(), insertRoom.getRid());
		// 없으면 생성
		if (entran == null) {
			entran = new Entrant();
			entran.setUid(insertRoom.getUid());
			entran.setRid(insertRoom.getRid());

			String queueName = "member." + Integer.toString(insertRoom.getUid());
			String roomName = "room." + Integer.toString(insertRoom.getRid());

			try {
				Queue queue = new Queue(queueName);
				FanoutExchange fanout = new FanoutExchange(roomName);
				fanout.setShouldDeclare(true);
				Binding bind = BindingBuilder.bind(queue).to(fanout);
				admin.declareBinding(bind);

				entran = entrantRepository.save(entran);
				return new ResponseEntity<>(entran, HttpStatus.OK);
			} catch (Exception e) {
				return new ResponseEntity<>("fail", HttpStatus.BAD_REQUEST);
			}
		}
		// 있으면 찾은거 그냥 반환
		return new ResponseEntity<>(entran, HttpStatus.OK);
	}

	@ApiOperation(value = "참가자 삭제 삭제")
	@Transactional
	@DeleteMapping(path = "/deleteByEid")
	public ResponseEntity<Object> deleteByUid(@RequestParam int eid) {
		Entrant entran = entrantRepository.findByEid(eid);
		String queueName = "member." + Integer.toString(entran.getUid());
		String roomName = "room." + Integer.toString(entran.getRid());

		try {
			Queue queue = new Queue(queueName, false);
			FanoutExchange fanout = new FanoutExchange(roomName);
			Binding bind = BindingBuilder.bind(queue).to(fanout);
			admin.removeBinding(bind);

			entrantRepository.deleteByEid(eid);
		} catch (Exception e) {
			return new ResponseEntity<>("fail", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>("success", HttpStatus.OK);
	}

	@ApiOperation(value = "참가자 수정")
	@Transactional
	@PutMapping(path = "/updateByEid")
	public ResponseEntity<Object> updateTheRoom(@RequestBody UpdateEntrantDto entranDto) {
		Entrant entrant = entrantRepository.findByEid(entranDto.getEid());

		if (entrant == null)
			return new ResponseEntity<>("참가자 명단이 없습니다", HttpStatus.OK);

		// 엑세스 토큰을 받아서 개설자인지 확인을 해야될까?
		entrant.setUid(entranDto.getUid());
		entrant.setRid(entranDto.getRid());

		try {
			entrantRepository.save(entrant);
		} catch (Exception e) {
			return new ResponseEntity<>("fail", HttpStatus.BAD_GATEWAY);
		}
		return new ResponseEntity<>("success", HttpStatus.OK);
	}
}