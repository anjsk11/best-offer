package me.anjsk.bestoffer.event;

import me.anjsk.bestoffer.domain.User;

public record AuctionCompletedEvent(
        Long auctionId,
        User seller,
        User buyer,
        Long finalPrice
) {
}