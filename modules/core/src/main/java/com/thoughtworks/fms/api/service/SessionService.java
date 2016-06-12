package com.thoughtworks.fms.api.service;

import javax.servlet.http.HttpServletRequest;

public class SessionService {

    public void setSessionAttribute(HttpServletRequest request, String key, Object value) {
        request.getSession().setAttribute(key, value);
    }

    public void invalidate(HttpServletRequest request) {
        request.getSession().invalidate();
    }

    public Object getAttribute(HttpServletRequest request, String key) {
        return request.getSession().getAttribute(key);
    }

    public void removeAttribute(HttpServletRequest request,  String key) {
        request.getSession().removeAttribute(key);
    }

}
