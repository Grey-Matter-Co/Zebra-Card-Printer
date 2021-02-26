package isv.zebra.com.zebracardprinter.zebra.jobStatus

import com.zebra.sdk.common.card.enumerations.CardSource

class JobInfo
{
	private var jobId: Int? = null
	private var cardSource: CardSource? = null

	constructor(cardSource: CardSource?)
		{ this.cardSource = cardSource }

	constructor (jobId: Int?, cardSource: CardSource?)
	{
		this.jobId = jobId
		this.cardSource = cardSource
	}

	fun getJobId(): Int?
		= jobId
	fun setJobId(jobId: Int?)
		{ this.jobId = jobId }

	fun getCardSource(): CardSource?
		= cardSource
}