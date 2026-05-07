import SwiftUI
import shared

struct UserListView: View {
    @StateObject private var observable: UserListObservable = UserListObservable()

    var body: some View {
        NavigationStack {
            contentView
                .navigationTitle("GitHub Users")
                .navigationDestination(item: $observable.selectedUser) { user in
                    UserDetailView(user: user)
                }
        }
    }

    /// Root content — phân nhánh theo `statusState` hiện tại.
    ///
    /// Tách thành `@ViewBuilder` riêng để tránh Swift compiler crash ("failed to produce
    /// diagnostic") khi type-check một view body quá phức tạp trong một block duy nhất.
    @ViewBuilder
    private var contentView: some View {
        if observable.state.statusState is StatusState.Loading {
            ProgressView("Đang tải...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = observable.state.statusState as? StatusState.Error {
            errorView(message: error.message ?? "Đã có lỗi xảy ra")
        } else {
            userListView
        }
    }

    /// Hiển thị danh sách users hoặc empty state khi không có dữ liệu.
    ///
    /// `dataState?.users` là optional vì SKIE erase generic type parameter của
    /// `ViewModelState<T>` khi bridge sang Swift — cần dùng `??` để unwrap an toàn.
    @ViewBuilder
    private var userListView: some View {
        if (observable.state.dataState?.users == nil) {
            ContentUnavailableView(
                "Không có user",
                systemImage: "person.slash"
            )
        } else {
            List(observable.state.dataState?.users ?? [], id: \.id) { user in
                UserRowView(user: user)
                    .onTapGesture {
                        observable.selectUser(user: user)
                    }
            }
            .listStyle(.plain)
        }
    }

    /// Hiển thị thông báo lỗi và nút "Thử lại".
    ///
    /// - Parameter message: Chuỗi lỗi từ `StatusState.Error.message` (đã unwrap optional).
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .foregroundStyle(.red)
                .multilineTextAlignment(.center)
                .padding()
            Button("Thử lại") {
                observable.loadUsers()
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
