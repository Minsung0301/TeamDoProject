package com.TeamAA.TeamDo.controller.Team;

import com.TeamAA.TeamDo.dto.Team.TeamResponse;
import com.TeamAA.TeamDo.service.Team.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // ✅ 팀 생성 (생성자가 팀장)
    @Operation(
            summary = "팀 생성",
            description = """
                    [결론]
                    새로운 팀을 생성하며, 팀 생성자는 자동으로 팀장 역할을 갖습니다.
                    생성된 팀은 고유한 초대코드를 포함합니다.

                    [사용 화면]
                    - 마이페이지 > 팀 생성 버튼

                    [주의사항]
                    - 팀 이름 중복 불가
                    - 로그인된 사용자만 가능

                    [참조 테이블]
                    - INSERT: team, team_participating
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 생성 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"teamId\":1,\"name\":\"팀A\",\"inviteCode\":\"ABC123\"}")
                    )),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"팀 이름을 입력해주세요.\"}")
                    )),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":401,\"message\":\"로그인이 필요합니다.\"}")
                    ))
    })
    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestParam String name, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            TeamResponse team = teamService.createTeam(name, userId);
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 내 팀 목록 조회
    @Operation(
            summary = "내 팀 목록 조회",
            description = """
                    [결론]
                    현재 로그인한 사용자가 속한 모든 팀 목록을 조회합니다.

                    [사용 화면]
                    - 마이페이지 > 팀 목록 조회 버튼

                    [주의사항]
                    - 로그인된 사용자만 조회 가능

                    [참조 테이블]
                    - SELECT: team_participating, team
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "[{\"teamId\":1,\"name\":\"팀A\"},{\"teamId\":2,\"name\":\"팀B\"}]")
                    )),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":401,\"message\":\"로그인이 필요합니다.\"}")
                    ))
    })
    @GetMapping("/teams")
    public ResponseEntity<?> getMyTeams(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        List<TeamResponse> teams = teamService.getMyTeams(userId);
        return ResponseEntity.ok(teams);
    }

    // ✅ 팀 상세 조회 (팀원만 가능)
    @Operation(
            summary = "팀 상세 조회",
            description = """
                    [결론]
                    팀원만 팀의 상세 정보를 조회할 수 있습니다.

                    [사용 화면]
                    - 팀 페이지 > 상세 조회 버튼

                    [주의사항]
                    - 요청 사용자가 팀원인지 검증
                    - 팀원이 아니면 접근 불가

                    [참조 테이블]
                    - SELECT: team, team_participating, user
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"teamId\":1,\"name\":\"팀A\",\"members\":[\"user1\",\"user2\"]}")
                    )),
            @ApiResponse(responseCode = "403", description = "팀원만 조회 가능",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":403,\"message\":\"팀원만 조회 가능합니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 팀 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"팀을 찾을 수 없습니다.\"}")
                    ))
    })
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getTeamDetail(@PathVariable Long teamId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            TeamResponse teamDto = teamService.getTeamDetailDto(teamId);
            return ResponseEntity.ok(teamDto);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.equals("팀을 찾을 수 없습니다.")) return ResponseEntity.status(404).body(msg);
            if (msg.equals("팀원만 조회 가능합니다.")) return ResponseEntity.status(403).body(msg);
            return ResponseEntity.badRequest().body(msg);
        }
    }

    // ✅ 초대코드로 팀 참가
    @Operation(
            summary = "초대코드로 팀 참가",
            description = """
                    [결론]
                    유효한 초대코드를 입력하면 해당 팀에 참여할 수 있습니다.

                    [사용 화면]
                    - 마이페이지 > 초대코드 입력 후 팀 참가버튼

                    [주의사항]
                    - 이미 참여 중이면 중복 참여 불가

                    [참조 테이블]
                    - SELECT: team
                    - INSERT: team_participating
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 참가 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"팀 참가 완료\"}")
                    )),
            @ApiResponse(responseCode = "400", description = "잘못된 또는 만료된 초대코드",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"유효하지 않은 초대코드입니다.\"}")
                    )),
            @ApiResponse(responseCode = "409", description = "이미 팀에 참여 중",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":409,\"message\":\"이미 팀에 참여중입니다.\"}")
                    ))
    })
    @PostMapping("/teams/join/by-invite-code")
    public ResponseEntity<?> joinTeam(@RequestParam String inviteCode, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            teamService.joinTeamByInviteCode(userId, inviteCode);
            return ResponseEntity.ok("팀 참가 완료");
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.equals("이미 팀에 참여중입니다.")) return ResponseEntity.status(409).body(msg);
            return ResponseEntity.badRequest().body(msg);
        }
    }

    // ✅ 초대코드 재발급 (팀장만 가능)
    @Operation(
            summary = "초대코드 재발급 (팀장 전용)",
            description = """
                    [결론]
                    팀장은 새로운 초대코드를 재발급할 수 있으며, 기존 초대코드는 즉시 무효화됩니다.

                    [사용 화면]
                    - 팀 상세 페이지 > 초대코드 재발급 버튼

                    [주의사항]
                    - 팀장만 가능

                    [참조 테이블]
                    - UPDATE: team 
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대코드 재발급 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"newInviteCode\":\"XYZ789\"}")
                    )),
            @ApiResponse(responseCode = "403", description = "팀장만 사용 가능",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":403,\"message\":\"팀장이 아니면 초대코드를 재발급할 수 없습니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 팀",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"팀을 찾을 수 없습니다.\"}")
                    ))
    })
    @PutMapping("/teams/{teamId}/invite-code")
    public ResponseEntity<?> regenerateInviteCode(@PathVariable Long teamId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            String newCode = teamService.regenerateInviteCode(teamId, userId);
            return ResponseEntity.ok("새 초대코드 : " + newCode);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.equals("팀을 찾을 수 없습니다.")) return ResponseEntity.status(404).body(msg);
            if (msg.equals("팀장이 아니면 초대코드를 재발급할 수 없습니다.")) return ResponseEntity.status(403).body(msg);
            return ResponseEntity.badRequest().body(msg);
        }
    }

    // ✅ 팀 나가기 (팀장은 불가능)
    @Operation(
            summary = "팀 나가기(팀장은 불가능)",
            description = """
                    [결론]
                    팀원은 팀을 자유롭게 나갈 수 있지만, 팀장은 팀을 나갈 수 없습니다.

                    [사용 화면]
                    - 팀 상세 페이지 > 팀 나가기 버튼

                    [주의사항]
                    - 팀장은 탈퇴 불가

                    [참조 테이블]
                    - DELETE: team_participating
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팀 나가기 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"팀에서 나갔습니다.\"}")
                    )),
            @ApiResponse(responseCode = "403", description = "팀장은 팀을 나갈 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":403,\"message\":\"팀장은 팀을 나갈 수 없습니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 팀에 속해 있지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"팀 참여 정보를 찾을 수 없습니다.\"}")
                    ))
    })
    @DeleteMapping("/teams/{teamId}/leave")
    public ResponseEntity<?> leaveTeam(@PathVariable Long teamId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            teamService.leaveTeam(userId, teamId);
            return ResponseEntity.ok("팀에서 나갔습니다.");
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.equals("팀장은 팀을 나갈 수 없습니다.")) return ResponseEntity.status(403).body(msg);
            if (msg.equals("팀 참여 정보를 찾을 수 없습니다.")) return ResponseEntity.status(404).body(msg);
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
