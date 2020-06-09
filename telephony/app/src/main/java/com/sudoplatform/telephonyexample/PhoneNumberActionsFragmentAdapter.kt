package com.sudoplatform.telephonyexample

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.sudoplatform.sudotelephony.PhoneNumber

class PhoneNumberActionsFragmentAdapter(private var app: App,
                                        private var number: PhoneNumber,
                                        private var tabCount: Int,
                                        fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ConversationListFragment.newInstance(app, number)
            1 -> PhoneCallListFragment.newInstance(app, number)
            else -> Fragment()
        }
    }

    override fun getCount(): Int {
        return tabCount
    }

}