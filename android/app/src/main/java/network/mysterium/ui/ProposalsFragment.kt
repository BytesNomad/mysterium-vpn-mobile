/*
 * Copyright (C) 2019 The "mysteriumnetwork/mysterium-vpn-mobile" Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package network.mysterium.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.makeramen.roundedimageview.RoundedImageView
import network.mysterium.AppContainer
import network.mysterium.MainApplication
import network.mysterium.service.core.ProposalPaymentMoney
import network.mysterium.ui.list.BaseItem
import network.mysterium.ui.list.BaseListAdapter
import network.mysterium.ui.list.BaseViewHolder
import network.mysterium.vpn.R

class ProposalsFragment : Fragment() {

    private lateinit var proposalsViewModel: ProposalsViewModel
    private lateinit var appContainer: AppContainer

    private lateinit var proposalsListRecyclerView: RecyclerView
    private lateinit var proposalsCloseButton: TextView
    private lateinit var proposalsSearchInput: EditText
    private lateinit var proposalsSwipeRefresh: SwipeRefreshLayout
    private lateinit var proposalsProgressBar: ProgressBar
    private lateinit var proposalsFilterCountry: LinearLayout
    private lateinit var proposalsFilterPrice: LinearLayout
    private lateinit var proposalsFilterQuality: LinearLayout
    private lateinit var proposalsFilterNodeType: LinearLayout
    private lateinit var proposalsFilterLayout: ConstraintLayout
    private lateinit var proposalsFilterCountryValue: TextView
    private lateinit var proposalsFilterPriceValue: TextView
    private lateinit var proposalsFilterQualityValue: TextView
    private lateinit var proposalsFilterNodeTypeValue: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_proposals, container, false)

        appContainer = (requireActivity().application as MainApplication).appContainer
        proposalsViewModel = appContainer.proposalsViewModel

        // Initialize UI elements.
        proposalsListRecyclerView = root.findViewById(R.id.proposals_list)
        proposalsCloseButton = root.findViewById(R.id.proposals_close_button)
        proposalsSearchInput = root.findViewById(R.id.proposals_search_input)
        proposalsSwipeRefresh = root.findViewById(R.id.proposals_list_swipe_refresh)
        proposalsProgressBar = root.findViewById(R.id.proposals_progress_bar)
        proposalsFilterCountry = root.findViewById(R.id.proposals_filter_country_layoyt)
        proposalsFilterLayout = root.findViewById(R.id.proposals_filters_layout)
        proposalsFilterPrice = root.findViewById(R.id.proposals_filter_price_layout)
        proposalsFilterCountryValue = root.findViewById(R.id.proposals_filter_country_value)
        proposalsFilterPriceValue = root.findViewById(R.id.proposals_filter_price_value)
        proposalsFilterQualityValue = root.findViewById(R.id.proposals_filter_quality_value)
        proposalsFilterNodeTypeValue = root.findViewById(R.id.proposals_filter_node_type_value)
        proposalsFilterQuality = root.findViewById(R.id.proposals_filter_quality_layout)
        proposalsFilterNodeType = root.findViewById(R.id.proposals_filter_node_type_layout)

        proposalsFilterCountry.setOnClickListener {
            navigateTo(root, Screen.PROPOSALS_COUNTRY_FILTER_LIST)
        }

        proposalsFilterPrice.setOnClickListener {
            navigateTo(root, Screen.PROPOSALS_PRICE_FILTER)
        }

        proposalsFilterQuality.setOnClickListener {
            navigateTo(root, Screen.PROPOSALS_QUALITY_FILTER)
        }

        proposalsFilterNodeType.setOnClickListener {
            navigateTo(root, Screen.PROPOSALS_NODE_TYPE_FILTER)
        }

        proposalsFilterLayout.visibility = View.GONE

        initProposalsList(root)
        initProposalsSearchFilter()

        proposalsCloseButton.setOnClickListener { handleClose(root) }
        onBackPress {
            navigateTo(root, Screen.MAIN)
        }

        return root
    }

    private fun navigateToMainVpnFragment(root: View) {
        navigateTo(root, Screen.MAIN)
    }

    private fun initProposalsSearchFilter() {
        if (proposalsViewModel.filter.searchText != "") {
            proposalsSearchInput.setText(proposalsViewModel.filter.searchText)
        }

        proposalsSearchInput.onChange { proposalsViewModel.filterBySearchText(it) }
    }

    private fun initProposalsList(root: View) {

        val listAdapter = BaseListAdapter{ selectedproposal ->
            if (selectedproposal is ProposalItem) {
                handleSelectedProposal(root, selectedproposal.uniqueId)
            }
        }
        proposalsListRecyclerView.adapter = listAdapter
        proposalsListRecyclerView.layoutManager = LinearLayoutManager(context)
        proposalsListRecyclerView.addItemDecoration(DividerItemDecoration(root.context, DividerItemDecoration.VERTICAL))
        proposalsSwipeRefresh.setOnRefreshListener {
            proposalsViewModel.refreshProposals {
                proposalsSwipeRefresh.isRefreshing = false
            }
        }

        // Subscribe to proposals changes.
        proposalsViewModel.getFilteredProposals().observe(viewLifecycleOwner, Observer { newItems ->
            listAdapter.submitList(createProposalItemsWithGroups(root, newItems))
            listAdapter.notifyDataSetChanged()

            // Hide progress bar once proposals are loaded.
            proposalsListRecyclerView.visibility = View.VISIBLE
            proposalsProgressBar.visibility = View.GONE
            proposalsFilterLayout.visibility = View.VISIBLE
            setSelectedFilterValues()
        })


        proposalsViewModel.initialProposalsLoaded.observe(viewLifecycleOwner, Observer {loaded ->
            if (loaded) {
                return@Observer
            }

            // If initial proposals failed to load during app init try to load them explicitly.
            proposalsListRecyclerView.visibility = View.GONE
            proposalsProgressBar.visibility = View.VISIBLE
            proposalsViewModel.refreshProposals {}
        })
    }

    private fun createProposalItemsWithGroups(root: View, proposals: List<ProposalViewItem>): MutableList<BaseItem> {
        val itemsWithHeaders = mutableListOf<BaseItem>()
        val groups = proposalsViewModel.groupedProposals(proposals)
        groups.forEach { group ->
            itemsWithHeaders.add(ProposalHeaderItem(group.title))
            group.children.forEach { proposal ->
                itemsWithHeaders.add(ProposalItem(root.context, proposal))
            }
        }
        return itemsWithHeaders
    }

    private fun setSelectedFilterValues() {
        val filter = proposalsViewModel.filter

        // Country filter value.
        proposalsFilterCountryValue.text = if (filter.country.name != "") {
            filter.country.name
        } else {
            getString(R.string.proposals_filter_country_value_all)
        }

        // Quality filter value.
        if (filter.quality.qualityIncludeUnreachable) {
            proposalsFilterQualityValue.text = getString(R.string.proposals_filter_quality_value_any)
        } else {
            proposalsFilterQualityValue.text = when(filter.quality.level) {
                QualityLevel.UNKNOWN -> getString(R.string.quality_level_any)
                QualityLevel.HIGH -> getString(R.string.quality_level_high)
                QualityLevel.MEDIUM -> getString(R.string.quality_level_medium)
                QualityLevel.LOW -> getString(R.string.quality_level_low)
            }
        }

        // Price filter value.
        val pricePerMinute = PriceUtils.displayMoney(ProposalPaymentMoney(amount = filter.pricePerMinute, currency = "MYSTT"))
        val pricePerGiB = PriceUtils.displayMoney(ProposalPaymentMoney(amount = filter.pricePerGiB, currency = "MYSTT"))
        proposalsFilterPriceValue.text = getString(R.string.proposals_price_combined, pricePerMinute, pricePerGiB)

        // Node(IP) type filter value.
        proposalsFilterNodeTypeValue.text = when(filter.nodeType) {
            NodeType.ALL -> getString(R.string.node_type_all)
            NodeType.BUSINESS -> getString(R.string.node_type_business)
            NodeType.CELLULAR -> getString(R.string.node_type_cellular)
            NodeType.HOSTING -> getString(R.string.node_type_hosting)
            NodeType.RESIDENTIAL -> getString(R.string.node_type_residential)
        }
    }

    private fun handleClose(root: View) {
        hideKeyboard(root)
        navigateToMainVpnFragment(root)
    }

    private fun handleSelectedProposal(root: View, proposalID: String) {
        hideKeyboard(root)
        proposalsViewModel.selectProposal(proposalID)
        navigateToMainVpnFragment(root)
    }
}

data class ProposalItem(val ctx: Context, val item: ProposalViewItem) : BaseItem() {

    override val layoutId = R.layout.proposal_list_item

    override val uniqueId = item.id

    override fun bind(holder: BaseViewHolder) {
        super.bind(holder)

        val countryFlag: RoundedImageView = holder.containerView.findViewById(R.id.proposal_item_country_flag)
        val countryName: TextView = holder.containerView.findViewById(R.id.proposal_item_country_name)
        val providerID: TextView = holder.containerView.findViewById(R.id.proposal_item_provider_id)
        val qualityLevel: ImageView = holder.containerView.findViewById(R.id.proposal_item_quality_level)
        val nodeType: TextView = holder.containerView.findViewById(R.id.proposal_item_provider_node_type)
        val price: TextView = holder.containerView.findViewById(R.id.proposal_item_price)

        countryFlag.setImageBitmap(item.countryFlagImage)
        countryName.text = item.countryName
        providerID.text = item.providerID
        qualityLevel.setImageResource(item.qualityResID)
        nodeType.text = when(item.nodeType) {
            NodeType.ALL -> "(${ctx.getString(R.string.node_type_all)})"
            NodeType.BUSINESS -> "(${ctx.getString(R.string.node_type_business)})"
            NodeType.CELLULAR -> "(${ctx.getString(R.string.node_type_cellular)})"
            NodeType.HOSTING -> "(${ctx.getString(R.string.node_type_hosting)})"
            NodeType.RESIDENTIAL -> "(${ctx.getString(R.string.node_type_residential)})"
        }
        val pricePerMinute = PriceUtils.displayMoney(PriceUtils.pricePerMinute(item.payment))
        val pricePerGiB = PriceUtils.displayMoney(PriceUtils.pricePerGiB(item.payment))
        price.text = ctx.resources.getString(R.string.proposals_price_combined, pricePerMinute, pricePerGiB)
    }
}

data class ProposalHeaderItem(val title: String) : BaseItem() {

    override val layoutId = R.layout.proposal_list_header_item

    override val uniqueId = title

    override fun bind(holder: BaseViewHolder) {
        super.bind(holder)
        val headerText: TextView = holder.containerView.findViewById(R.id.proposal_item_header_text)
        headerText.text = title
    }
}
