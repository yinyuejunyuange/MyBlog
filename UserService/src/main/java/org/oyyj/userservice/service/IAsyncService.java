package org.oyyj.userservice.service;


import org.oyyj.userservice.DTO.BlogDTO;

public interface IAsyncService {
    void upLoadBlogToAI(BlogDTO blogDTO);
}
