package me.anjsk.bestoffer.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.annotation.RequireAdmin;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.exception.AdminRequiredException;
import me.anjsk.bestoffer.exception.LoginRequiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 요청이 컨트롤러의 메서드로 가는 게 아니면 통과 (예: 정적 파일 요청)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 호출하려는 메서드에 @RequireAdmin가 붙어있는지 확인
        boolean isAdminOnly = handlerMethod.hasMethodAnnotation(RequireAdmin.class);

        // @RequireAdmin 아니라면, 누구나 접근 가능
        if (!isAdminOnly) {
            return true;
        }

        // @RequireAdmin 이라면, 세션을 꺼내서 검사 시작
        HttpSession session = request.getSession(false);

        // 세션이 아예 없거나 로그인을 안 했으면? (401 에러)
        if (session == null || session.getAttribute("LOGIN_USER") == null) {
            throw new LoginRequiredException();
        }

        // 로그인은 했는데 관리자가 아니면? (403 에러)
        UserRole role = (UserRole) session.getAttribute("USER_ROLE");
        if (role != UserRole.ROLE_ADMIN) {
            throw new AdminRequiredException();
        }

        // 모든 검사를 통과하면 컨트롤러로 진입 허용
        return true;
    }
}