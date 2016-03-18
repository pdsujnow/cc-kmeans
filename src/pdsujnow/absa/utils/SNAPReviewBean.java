package pdsujnow.absa.utils;

/***
product/productId: asin, e.g. amazon.com/dp/B00006HAXW
product/title: title of the product
product/price: price of the product
review/userId: id of the user, e.g. A1RSDE90N6RSZF
review/profileName: name of the user
review/helpfulness: fraction of users who found the review helpful
review/score: rating of the product
review/time: time of the review (unix time)
review/summary: review summary
review/text: text of the review
 */
public class SNAPReviewBean {
	private String productId;
	private String title;
	private String price;
	private String userId;
	private String profileName;
	private String helpfulness;
	private String score;
	private String time;
	private String summary;
	private String text;

	@Override
	public String toString() {
		return "review/profileName:" + profileName + ", product/price:" + price
				+ ", review/time:" + time + ", product/productId" + productId
				+ ", review/helpfulness:" + helpfulness + ", review/summary:"
				+ summary + ", review/userId: " + userId + ", product/title:"
				+ title + ", review/score:" + score + ", review/text: " + text;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getHelpfulness() {
		return helpfulness;
	}

	public void setHelpfulness(String helpfulness) {
		this.helpfulness = helpfulness;
	}

	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
