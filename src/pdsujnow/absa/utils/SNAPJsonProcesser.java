package pdsujnow.absa.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pdsu.xsf.text.JsonUtils;

public class SNAPJsonProcesser {
	public static void main(String[] args) {
		JsonUtils json = new JsonUtils();
		String path = "G:\\SNAP\\Cell_Phones_&_Accessories.txt";
		
//		List<SNAPReviewBean> reviewList = new ArrayList<SNAPReviewBean>();
//		for (int i = 0; i < 10; i++) {
//			SNAPReviewBean review = new SNAPReviewBean();
//			review.setUserId("00"+i);
//			
//			reviewList.add(review);
//		}
		//json.saveJsonFile(path, userList);
		
		@SuppressWarnings({ "unchecked", "unused" })
		List<SNAPReviewBean> listReview = (List<SNAPReviewBean>) json.getJsonListObj(path, SNAPReviewBean.class);
		
//		listReview.get(0).setUserId("001-U3");		
		listReview.remove(0);		
//		json.saveJsonFile(path, listReview);
		
	}
}
