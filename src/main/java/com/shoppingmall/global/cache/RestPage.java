package com.shoppingmall.global.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Redis 캐싱 시 Page<T>를 직렬화/역직렬화하기 위한 래퍼 클래스.
 * PageImpl은 Jackson이 역직렬화할 수 없어서 @JsonCreator로 생성자를 명시한 커스텀 클래스가 필요하다.
 */
// pageable, sort는 역직렬화 시 재구성 불가 → 무시하고 number+size+totalElements로 재구성
@JsonIgnoreProperties(value = {"pageable", "sort"}, ignoreUnknown = true)
public class RestPage<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") int page,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long total) {
        super(content, PageRequest.of(page, size), total);
    }

    public RestPage(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }
}
