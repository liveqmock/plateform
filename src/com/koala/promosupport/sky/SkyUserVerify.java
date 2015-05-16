package com.koala.promosupport.sky;

import com.koala.promosupport.kola.KolaUserVerify;

/**
 * extends KolaUserVerify
 * @author AHONG
 *
 */
public class SkyUserVerify extends KolaUserVerify {

	private SkyChannel ch;
	
	public SkyUserVerify(SkyChannel ch){
		this.ch = ch;
	}

}
