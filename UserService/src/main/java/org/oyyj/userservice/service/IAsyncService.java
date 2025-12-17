package org.oyyj.userservice.service;


import org.oyyj.userservice.dto.BlogDTO;

public interface IAsyncService {
    void upLoadBlogToAI(BlogDTO blogDTO);
}
