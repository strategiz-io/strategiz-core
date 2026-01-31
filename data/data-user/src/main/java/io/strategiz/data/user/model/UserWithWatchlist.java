package io.strategiz.data.user.model;

import io.strategiz.data.user.entity.UserEntity;

/**
 * User with watchlist aggregated data
 */
public class UserWithWatchlist {

	private UserEntity user;

	private Object watchlist; // TODO: Will be typed when data-watchlist module is updated

	// Constructors
	public UserWithWatchlist() {
	}

	public UserWithWatchlist(UserEntity user, Object watchlist) {
		this.user = user;
		this.watchlist = watchlist;
	}

	// Getters and Setters
	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public Object getWatchlist() {
		return watchlist;
	}

	public void setWatchlist(Object watchlist) {
		this.watchlist = watchlist;
	}

}