package com.lusolaw.mapper;

import com.lusolaw.dto.BookingResponse;
import com.lusolaw.dto.LawyerResponse;
import com.lusolaw.dto.ServiceResponse;
import com.lusolaw.dto.UserSummaryResponse;
import com.lusolaw.model.Booking;
import com.lusolaw.model.Service;
import com.lusolaw.model.User;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAddress(),
                user.getSpecialization(),
                user.getLawyerRegistrationNumber(),
                user.getAccountStatus(),
                user.getPricePerHour(),
                user.getCreatedAt()
        );
    }

    public static LawyerResponse toLawyer(User user) {
        if (user == null) {
            return null;
        }
        return new LawyerResponse(
                user.getId(),
                user.getName(),
                user.getSpecialization(),
                user.getPricePerHour()
        );
    }

    public static ServiceResponse toService(Service service) {
        if (service == null) {
            return null;
        }
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                toLawyer(service.getLawyer()),
                service.getCreatedAt()
        );
    }

    public static BookingResponse toBooking(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingResponse(
                booking.getId(),
                toService(booking.getService()),
                toUserSummary(booking.getClient()),
                toLawyer(booking.getLawyer()),
                booking.getSituation(),
                booking.getDetails(),
                booking.getAmount(),
                booking.getStatus(),
                booking.getRequestedAt(),
                booking.getRespondedAt(),
                booking.getDeadline(),
                booking.getPaidAt()
        );
    }
}
