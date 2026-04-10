package com.web.lab.post.dto;

import com.web.lab.user.dto.PaginationMeta;

import java.util.List;

public class PostPagedResponse<T> {
    private List<T> data;
    private PaginationMeta meta;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public PaginationMeta getMeta() {
        return meta;
    }

    public void setMeta(PaginationMeta meta) {
        this.meta = meta;
    }
}
