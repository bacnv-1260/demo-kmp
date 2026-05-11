import Foundation
import shared

class UserListPresenter {
    let viewModel: UserListViewModel

    init() {
        viewModel = KoinHelper().getUserListViewModel()
    }

    func loadUsers() {
        viewModel.processIntent(intent: UserListIntent.LoadUsers())
    }

    func selectUser(user: GitHubUser) {
        viewModel.processIntent(intent: UserListIntent.SelectUser(user: user))
    }
}
