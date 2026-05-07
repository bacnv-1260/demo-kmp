import SwiftUI
import shared

struct UserDetailView: View {
    let user: GitHubUser

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                AsyncImage(url: URL(string: user.avatarUrl)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Circle()
                        .fill(Color.secondary.opacity(0.2))
                }
                .frame(width: 120, height: 120)
                .clipShape(Circle())

                Text(user.login)
                    .font(.title)
                    .bold()

                Label(user.type, systemImage: "person.fill")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.secondary.opacity(0.1), in: Capsule())

                if let url = URL(string: user.htmlUrl) {
                    Link(destination: url) {
                        Label("Xem trang GitHub", systemImage: "safari")
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity)
        }
        .navigationTitle(user.login)
        .navigationBarTitleDisplayMode(.inline)
    }
}
