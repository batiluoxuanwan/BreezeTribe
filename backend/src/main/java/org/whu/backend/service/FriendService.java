package org.whu.backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.friend.FriendDto;
import org.whu.backend.dto.friend.FriendRequestDto;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.association.friend.FriendRequest;
import org.whu.backend.entity.association.friend.Friendship;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.repository.authRepo.friend.FriendRequestRepository;
import org.whu.backend.repository.authRepo.friend.FriendShipRepository;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.JpaUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FriendService {
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendShipRepository friendShipRepository;

    @Autowired
    private AccountUtil accountUtil;
    @Autowired
    private AuthRepository accountRepository;

    public void sendRequest(String toAccountId) {
        String currentAccountId = AccountUtil.getCurrentAccountId();
        Optional<Account> toAccount = accountRepository.findById(toAccountId);
        if(toAccount.isEmpty())
            throw new BizException("用户不存在");
        Account currentAccount = accountUtil.getCurrentAccount();
        // 检查是否已是好友，是否已发送请求等等...
        FriendRequest request = new FriendRequest();
        request.setId(UUID.randomUUID().toString());
        request.setFrom(currentAccount);
        request.setTo(toAccount.get());
        request.setStatus(FriendRequest.RequestStatus.PENDING);
        friendRequestRepository.save(request);
    }

    public void acceptRequest(String requestId) {
        FriendRequest request = JpaUtil.getOrThrow(friendRequestRepository, requestId, "好友请求不存在");
//        if (!request.getReceiverId().equals(accountId)) {
//            throw new BizException("无权接受该好友请求");
//        }
        Account currentAccount = accountUtil.getCurrentAccount();
        //String currentAccountId = AccountUtil.getCurrentAccountId();
        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        // 创建双向好友记录
        Friendship friend1 = new Friendship();
        friend1.setAccount1(currentAccount);
        friend1.setAccount2(request.getTo());
        Friendship friend2 = new Friendship();
        friend2.setAccount1(request.getTo());
        friend2.setAccount2(currentAccount);
        friendShipRepository.save(friend1);
        friendShipRepository.save(friend2);
    }

    public void rejectRequest(String requestId) {
        FriendRequest request = JpaUtil.getOrThrow(friendRequestRepository, requestId, "好友请求不存在");
//        if (!request.getReceiverId().equals(accountId)) {
//            throw new BizException("无权接受该好友请求");
//        }
        Account currentAccount = accountUtil.getCurrentAccount();
        //String currentAccountId = AccountUtil.getCurrentAccountId();
        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepository.save(request);
    }

    public void deleteFriend(String friendId) {
        String accountId = AccountUtil.getCurrentAccountId();
        friendShipRepository.deleteByAccountIdAndFriendId(accountId, friendId);
        friendShipRepository.deleteByAccountIdAndFriendId(friendId, accountId);
    }

//    public List<FriendDTO> getFriends(String accountId) {
//        return friendRepository.findByAccountId(accountId).stream()
//                .map(friend -> new FriendDTO(friend.getFriendId()))
//                .toList();
//    }
    public PageResponseDto<FriendDto> getFriends(PageRequestDto pageRequestDto) {
        String accountId = AccountUtil.getCurrentAccountId();
        // 1️创建 Pageable
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));
        // 2️获取 Page<Friend> 实体
        Page<Friendship> page = friendShipRepository.findByAccount1(accountId, pageable);

        // 3️转换为 DTO
        List<FriendDto> content = page.getContent().stream()
                .map(friend -> {
                    FriendDto dto = new FriendDto();
                    dto.setId(friend.getId());
                    dto.setAccount1(friend.getAccount1());
                    dto.setAccount2(friend.getAccount2());
                    dto.setCreatedAt(friend.getCreatedAt());
                    return dto;
                })
                .toList();

        // 4️构建 PageResponseDto
        return PageResponseDto.<FriendDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }
//    public List<FriendRequestDTO> getFriendRequests(String accountId) {
//        return friendRequestRepository.findByReceiverId(accountId).stream()
//                .map(request -> new FriendRequestDTO(request.getId(), request.getSenderId(), request.getCreatedAt()))
//                .toList();
//    }
    public PageResponseDto<FriendRequestDto> getReceivedFriendRequests(PageRequestDto pageRequestDto) {
        String accountId = AccountUtil.getCurrentAccountId();
        // 1️创建 Pageable
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));
        // 2️获取 Page<FriendRequest> 实体
        Page<FriendRequest> page = friendRequestRepository.findByTo(accountId, pageable);

        // 3️转换为 DTO
        List<FriendRequestDto> content = page.getContent().stream()
                .map(request -> {
                    FriendRequestDto dto = new FriendRequestDto();
                    dto.setId(request.getId());
                    dto.setFrom(request.getFrom());           // 请求发起方
                    dto.setTo(request.getTo());               // 请求接收方
                    dto.setStatus(request.getStatus());       // 请求状态
                    dto.setCreatedAt(request.getCreatedAt()); // 创建时间
                    return dto;
                })
                .toList();

        // 4️构建 PageResponseDto
        return PageResponseDto.<FriendRequestDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }
    public PageResponseDto<FriendRequestDto> getSentFriendRequests(PageRequestDto pageRequestDto) {
        String accountId = AccountUtil.getCurrentAccountId();
        // 1️创建 Pageable
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));
        // 2️获取 Page<FriendRequest> 实体
        Page<FriendRequest> page = friendRequestRepository.findByFrom(accountId, pageable);

        // 3️转换为 DTO
        List<FriendRequestDto> content = page.getContent().stream()
                .map(request -> {
                    FriendRequestDto dto = new FriendRequestDto();
                    dto.setId(request.getId());
                    dto.setFrom(request.getFrom());           // 请求发起方
                    dto.setTo(request.getTo());               // 请求接收方
                    dto.setStatus(request.getStatus());       // 请求状态
                    dto.setCreatedAt(request.getCreatedAt()); // 创建时间
                    return dto;
                })
                .toList();

        // 4️构建 PageResponseDto
        return PageResponseDto.<FriendRequestDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }
}
