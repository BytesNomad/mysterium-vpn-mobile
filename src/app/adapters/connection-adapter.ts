/*
 * Copyright (C) 2018 The 'MysteriumNetwork/mysterium-vpn-mobile' Authors.
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

import { ConnectionStatusDTO } from 'mysterium-tequilapi'
import ConnectionStatistics from '../models/connection-statistics'
import Ip from '../models/ip'

// TODO: uncouple from mysterium-tequilapi by using domain models for response data
interface IConnectionAdapter {
  connect (consumerId: string, providerId: string): Promise<void>
  disconnect (): Promise<void>
  fetchStatus (): Promise<ConnectionStatusDTO>
  fetchStatistics (): Promise<ConnectionStatistics>
  fetchIp (): Promise<Ip>
}

export default IConnectionAdapter