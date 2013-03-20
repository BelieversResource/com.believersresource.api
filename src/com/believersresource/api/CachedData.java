package com.believersresource.api;

import com.believersresource.data.BibleBooks;
import com.believersresource.data.BibleTranslations;


public class CachedData {
	private static BibleBooks bibleBooks;
    private static BibleTranslations bibleTranslations;
    
	public static BibleBooks getBibleBooks()
	{
		if (bibleBooks==null) bibleBooks = BibleBooks.loadAll();
		return bibleBooks;
	}
	
	public static BibleTranslations getBibleTranslations()
	{
		if (bibleTranslations==null) bibleTranslations = BibleTranslations.loadAll();
		return bibleTranslations;
	}

}
