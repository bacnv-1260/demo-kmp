import Foundation
import shared

// Bridge: wraps UserDao và implements UserLocalDataSource (KMP interface)
// Giống pattern UserDaoLocalDataSource bên Android
class UserDaoLocalDataSource: UserLocalDataSource {
    
    private let userDao: UserDao
        
    init(userDao: UserDao) {
        self.userDao = userDao
    }

    func getAllUser() -> [GitHubUser] {
        return userDao.getAllUser().map { user in
            GitHubUser(
                id: Int32(user.id),
                login: user.login,
                avatarUrl: user.avatarUrl,
                htmlUrl: user.htmlUrl,
                type: user.type
            )
        }
    }
}
